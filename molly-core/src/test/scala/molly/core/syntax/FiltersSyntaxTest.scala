package molly.core.syntax
import com.mongodb.client.model.Filters
import molly.core.syntax.filters.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FiltersSyntaxTest extends AnyFlatSpec with Matchers {
   private val aFilter = Filters.eq("key_1", "value_1")
   private val anotherFilter = Filters.eq("key_2", "value_2")

   "The filters 'and' extension" should "performs a logical AND" in {
      aFilter and anotherFilter shouldEqual Filters.and(
         aFilter,
         anotherFilter
      )
   }

   "The filters 'or' extension" should "performs a logical OR" in {
      aFilter or anotherFilter shouldEqual Filters.or(
         aFilter,
         anotherFilter
      )
   }

   "The filters 'not' extension" should "performs a logical NOT" in {
      aFilter.not shouldEqual Filters.not(
         aFilter
      )
   }

   "The filters 'nor' extension" should "performs a logical NOR" in {
      aFilter.nor shouldEqual Filters.nor(
         aFilter
      )
   }
}
