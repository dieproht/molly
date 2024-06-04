package molly.core.syntax

import com.mongodb.client.model.Filters
import org.bson.conversions.Bson

trait filters:

  extension (filter: Bson)
    def and(anotherFilter: Bson): Bson = Filters.and(filter, anotherFilter)

    def or(anotherFilter: Bson): Bson = Filters.or(filter, anotherFilter)

    def not: Bson = Filters.not(filter)

    def nor: Bson = Filters.nor(filter)

object filters extends filters
