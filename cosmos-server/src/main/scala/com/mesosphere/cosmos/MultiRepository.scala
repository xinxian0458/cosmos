package com.mesosphere.cosmos

import com.mesosphere.cosmos.http.RequestSession
import com.mesosphere.cosmos.repository._
import com.mesosphere.universe
import com.netaporter.uri.Uri
import com.twitter.util.Future
import scala.util.matching.Regex

final class MultiRepository(
    packageRepositoryStorage: PackageSourcesStorage,
    universeClient: UniverseClient
)
    extends PackageCollection {

  @volatile private[this] var cachedRepositories =
    Map.empty[Uri, DefaultCosmosRepository]

  override def getPackagesByPackageName(
      packageName: String
  )(implicit session: RequestSession)
    : Future[List[internal.model.PackageDefinition]] = {
    /* Fold over all the results in order and ignore PackageNotFound errors.
     * We have found our answer when we find a PackageDefinition or a generic exception.
     */
    repositories().flatMap { repositories =>
      Future.collect {
        repositories.map { repository =>
          repository
            .getPackagesByPackageName(packageName)
            .map(Some(_))
            .handle {
              case PackageNotFound(_) => None
            }
        }
      } map (_.flatten)
    } map { packages =>
      packages.find(!_.isEmpty).getOrElse(throw PackageNotFound(packageName))
    }
  }

  override def getPackageByPackageVersion(
      packageName: String,
      packageVersion: Option[universe.v3.model.PackageDefinition.Version]
  )(implicit session: RequestSession)
    : Future[(internal.model.PackageDefinition, Uri)] = {
    /* Fold over all the results in order and ignore PackageNotFound and VersionNotFound errors.
     * We have found our answer when we find a PackageDefinition or a generic exception.
     */
    repositories().flatMap { repositories =>
      Future.collect {
        repositories.map { repository =>
          repository
            .getPackageByPackageVersion(packageName, packageVersion)
            .map(Some(_))
            .handle {
              case PackageNotFound(_) => None
              case VersionNotFound(_, _) => None
            }
        }
      } map (_.flatten)
    } map { packages =>
      packages.headOption.getOrElse {
        packageVersion match {
          case Some(version) => throw VersionNotFound(packageName, version)
          case None => throw PackageNotFound(packageName)
        }
      }
    }
  }

  override def search(
      query: Option[String]
  )(implicit session: RequestSession)
    : Future[List[rpc.v1.model.SearchResult]] = {
    repositories().flatMap { repositories =>
      val searches = repositories.zipWithIndex.map {
        case (repository, repositoryIndex) =>
          repository.search(query).map { results =>
            results.map(_ -> repositoryIndex)
          }
      }

      val fSearchResults: Future[List[(rpc.v1.model.SearchResult, Int)]] =
        Future.collect(searches).map(_.toList.flatten)

      fSearchResults.map { results =>
        results.groupBy {
          case (searchResult, _) => searchResult.name
        }.map {
          case (name, list) =>
            val (searchResult, _) = list.sortBy { case (_, index) => index }.head
            searchResult
        }.toList
      }
    }
  }

  def getRepository(uri: Uri): Future[Option[CosmosRepository]] = {
    repositories().map { repositories =>
      repositories.find(_.repository.uri == uri)
    }
  }

  private[this] def repositories(): Future[List[DefaultCosmosRepository]] = {
    packageRepositoryStorage.readCache().map { repositories =>
      val oldRepositories: Map[Uri, DefaultCosmosRepository] =
        cachedRepositories
      val result = repositories.map { repository =>
        oldRepositories.getOrElse(repository.uri,
                                  CosmosRepository(repository, universeClient))
      }

      val newRepositories: Map[Uri, DefaultCosmosRepository] = result
        .map(cosmosRepository =>
              (cosmosRepository.repository.uri, cosmosRepository))
        .toMap

      /* Note: This is an optimization. We always get the latest inventory of repository from
       * the storage.
       */
      cachedRepositories = newRepositories

      result
    }
  }

  def packages(implicit session: RequestSession)
    : Future[List[List[internal.model.PackageDefinition]]] = {
    repositories().flatMap { repositories =>
      Future.collect(repositories.map(_.packages)).map(_.toList)
    }
  }
}

object MultiRepository {
  def search(repositories: List[List[internal.model.PackageDefinition]],
             query: Option[String]): List[rpc.v1.model.SearchResult] = {
    val predicate =
      query.map { value =>
        if (value.contains("*")) {
          searchRegex(createRegex(value))
        } else {
          searchString(value.toLowerCase())
        }
      } getOrElse { (_: rpc.v1.model.SearchResult) =>
        true
      }

    val searchesWithIndex = for {
      (packages, index) <- repositories.zipWithIndex
      search <- reduceToSearches(packages)
      if predicate(search)
    } yield (search, index)

    searchesWithIndex.groupBy {
      case (search, _) => search.name
    }.map {
      case (name, searchesWithIndex) =>
        val (search, _) = searchesWithIndex.sortBy { case (_, index) => index }.head
        search
    }.toList
  }

  private[this] def reduceToSearches(
      packages: List[internal.model.PackageDefinition])
    : List[rpc.v1.model.SearchResult] = {
    val result = packages
      .groupBy(_.name)
      .map {
        case (_, sameNamePkges) =>
          val versions = sameNamePkges
            .groupBy(_.version)
            .toMap
            .mapValues(sameVersionPkges =>
                  sameVersionPkges.map(_.releaseVersion).max)

          val latestPackage = sameNamePkges.sortBy(_.releaseVersion).last

          rpc.v1.model.SearchResult(latestPackage.name,
                                    latestPackage.version,
                                    versions,
                                    latestPackage.description,
                                    latestPackage.framework.getOrElse(false),
                                    latestPackage.tags,
                                    latestPackage.selected,
                                    latestPackage.resource.flatMap(_.images))
      }
      .toList

    result
  }

  private[this] def createRegex(query: String): Regex = {
    s"""^${query.replaceAll("\\*", ".*")}$$""".r
  }

  private[this] def searchRegex(
      regex: Regex): rpc.v1.model.SearchResult => Boolean = { pkg =>
    regex.findFirstIn(pkg.name).isDefined ||
    regex.findFirstIn(pkg.description).isDefined ||
    pkg.tags.exists(tag => regex.findFirstIn(tag.value).isDefined)
  }

  private[this] def searchString(
      query: String): rpc.v1.model.SearchResult => Boolean = { pkg =>
    pkg.name.toLowerCase().contains(query) ||
    pkg.description.toLowerCase().contains(query) ||
    pkg.tags.exists(tag => tag.value.toLowerCase().contains(query))
  }
}
