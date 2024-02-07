package molly.core.syntax

import com.mongodb.client.model.Updates
import org.bson.conversions.Bson

import scala.language.implicitConversions

object update {
   implicit def mollyUpdatesSyntax(update: Bson): UpdatesExtensions =
      new UpdatesExtensions(update)
}

final class UpdatesExtensions(update: Bson) {
   def combine(anotherUpdate: Bson): Bson = Updates.combine(update, anotherUpdate)

}
