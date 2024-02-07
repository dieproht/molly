package molly.core.syntax

import com.mongodb.client.model.Updates
import molly.core.syntax.update.mollyUpdatesSyntax
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UpdatesSyntaxTest extends AnyFlatSpec with Matchers {
   private val aSetUpdate = Updates.set("key_1", "value_1")
   private val anUnsetUpdate = Updates.unset("key_2")

   "The updates 'combine' extension" should "combine both given updates" in {
      aSetUpdate combine anUnsetUpdate shouldEqual Updates.combine(
         aSetUpdate,
         anUnsetUpdate
      )
   }

}
