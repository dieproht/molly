package molly.core.syntax

import com.mongodb.client.model.Filters
import org.bson.conversions.Bson

import scala.language.implicitConversions

trait filters:
  implicit def mollyFiltersSyntax(filter: Bson): FiltersExtensions =
    new FiltersExtensions(filter)

object filters extends filters

final class FiltersExtensions(filter: Bson):
  def and(anotherFilter: Bson): Bson = Filters.and(filter, anotherFilter)

  def or(anotherFilter: Bson): Bson = Filters.or(filter, anotherFilter)

  def not: Bson = Filters.not(filter)

  def nor: Bson = Filters.nor(filter)
