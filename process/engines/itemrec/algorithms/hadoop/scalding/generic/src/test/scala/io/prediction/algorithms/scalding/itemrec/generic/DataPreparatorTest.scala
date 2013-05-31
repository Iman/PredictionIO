package io.prediction.algorithms.scalding.itemrec.generic

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Items, U2iActions}
import io.prediction.commons.filepath.DataFile

class DataPreparatorTest extends Specification with TupleConversions {
  
  val Rate = 0
  val LikeDislike = 1
  val View = 2
  //val ViewDetails = 3
  val Conversion = 4
  
  def test(itypes: List[String], params: Map[String, String],
      items: List[(String, String)], u2iActions: List[(Int, String, String, String, String)],
      ratings: List[(String, String, Int)], selectedItems: List[(String, String)]) = {
    
    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None //Option("testhost")
    val dbPort = None //Option(27017)
    val hdfsRoot = "testroot/"
    
    JobTest("io.prediction.algorithms.scalding.itemrec.generic.DataPreparator")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      //.arg("dbHost", dbHost.get)
      //.arg("dbPort", dbPort.get.toString)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", "2")
      .arg("engineid", "4")
      .arg("algoid", "5")
      .arg("itypes", itypes)
      .arg("viewParam", params("viewParam"))
      .arg("likeParam", params("likeParam"))
      .arg("dislikeParam", params("dislikeParam"))
      .arg("conversionParam", params("conversionParam"))
      .arg("conflictParam", params("conflictParam"))
      //.arg("debug", List("test")) // NOTE: test mode
      .source(Items(appId=2, itypes=Some(itypes), dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, items)
      .source(U2iActions(appId=2, dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, u2iActions)
      .sink[(String, String, Int)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "ratings.tsv"))) { outputBuffer =>
        "correctly process and write data to ratings.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(ratings)
        }
      }
      .sink[(String, String)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "selectedItems.tsv"))) { outputBuffer =>
        "correctly write selectedItems.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(selectedItems)
        }
      }
      .run
      .finish
    
  }
  
  /** no itypes specified */
  def testWithoutItypes(params: Map[String, String],
      items: List[(String, String)], u2iActions: List[(Int, String, String, String, String)],
      ratings: List[(String, String, Int)], selectedItems: List[(String, String)]) = {
    
    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None //Option("testhost")
    val dbPort = None //Option(27017)
    val hdfsRoot = "testroot/"
    
    JobTest("io.prediction.algorithms.scalding.itemrec.generic.DataPreparator")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      //.arg("dbHost", dbHost.get)
      //.arg("dbPort", dbPort.get.toString)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", "2")
      .arg("engineid", "4")
      .arg("algoid", "5")
      //.arg("itypes", itypes) // NOTE: no itypes args!
      .arg("viewParam", params("viewParam"))
      .arg("likeParam", params("likeParam"))
      .arg("dislikeParam", params("dislikeParam"))
      .arg("conversionParam", params("conversionParam"))
      .arg("conflictParam", params("conflictParam"))
      //.arg("debug", List("test")) // NOTE: test mode
      .source(Items(appId=2, itypes=None, dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, items)
      .source(U2iActions(appId=2, dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, u2iActions)
      .sink[(String, String, Int)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "ratings.tsv"))) { outputBuffer =>
        "correctly process and write data to ratings.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(ratings)
        }
      }
      .sink[(String, String)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "selectedItems.tsv"))) { outputBuffer =>
        "correctly write selectedItems.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(selectedItems)
        }
      }
      .run
      .finish
    
  }
  
  /**
   * Test 1. basic. Rate actions only without conflicts
   */
  val test1AllItypes = List("t1", "t2", "t3", "t4")
  val test1Items = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test1U2i = List(
      (Rate, "u0", "i0", "123450", "3"), 
      (Rate, "u0", "i1", "123457", "1"),
      (Rate, "u0", "i2", "123458", "4"),
      (Rate, "u0", "i3", "123459", "2"),
      (Rate, "u1", "i0", "123457", "5"),
      (Rate, "u1", "i1", "123458", "2"))
      
  val test1Ratings = List(      
      ("u0", "i0", 3), 
      ("u0", "i1", 1),
      ("u0", "i2", 4),
      ("u0", "i3", 2),
      ("u1", "i0", 5),
      ("u1", "i1", 2))
  
  val test1Params: Map[String, String] = Map("viewParam" -> "3", "likeParam" -> "4", "dislikeParam" -> "1", "conversionParam" -> "5",
      "conflictParam" -> "latest") 
  
  "itemrec.generic DataPreparator with only rate actions, all itypes, no conflict" should {
    test(test1AllItypes, test1Params, test1Items, test1U2i, test1Ratings, test1Items)
  }
  
  "itemrec.generic DataPreparator with only rate actions, no itypes specified, no conflict" should {
    testWithoutItypes(test1Params, test1Items, test1U2i, test1Ratings, test1Items)
  }
  
  /**
   * Test 2. rate actions only with conflicts
   */
  val test2AllItypes = List("t1", "t2", "t3", "t4")
  val test2Items = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test2U2i = List(
      (Rate, "u0", "i0", "123448", "3"),
      (Rate, "u0", "i0", "123449", "4"), // highest
      (Rate, "u0", "i0", "123451", "2"), // latest 
      (Rate, "u0", "i0", "123450", "1"), // lowest
      
      (Rate, "u0", "i1", "123456", "1"), // lowest
      (Rate, "u0", "i1", "123457", "2"),
      (Rate, "u0", "i1", "123458", "3"), // latest, highest

      (Rate, "u0", "i2", "123461", "2"), // latest, lowest
      (Rate, "u0", "i2", "123459", "3"),
      (Rate, "u0", "i2", "123460", "5"), // highest
      
      (Rate, "u0", "i3", "123459", "2"),
      (Rate, "u1", "i0", "123457", "5"),
      
      (Rate, "u1", "i1", "123458", "3"), // lowest
      (Rate, "u1", "i1", "123459", "4"), // highest
      (Rate, "u1", "i1", "123460", "3")) // latest, lowest
      
  val test2RatingsLatest = List(
      ("u0", "i0", 2), 
      ("u0", "i1", 3),
      ("u0", "i2", 2),
      ("u0", "i3", 2),
      ("u1", "i0", 5),
      ("u1", "i1", 3))
  
   val test2RatingsHighest = List(
      ("u0", "i0", 4), 
      ("u0", "i1", 3),
      ("u0", "i2", 5),
      ("u0", "i3", 2),
      ("u1", "i0", 5),
      ("u1", "i1", 4))

   val test2RatingsLowest = List(
      ("u0", "i0", 1), 
      ("u0", "i1", 1),
      ("u0", "i2", 2),
      ("u0", "i3", 2),
      ("u1", "i0", 5),
      ("u1", "i1", 3))
      
  val test2Itypes_t1t4 = List("t1", "t4")
  val test2Items_t1t4 = List(("i0", "t1,t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test2RatingsHighest_t1t4 = List(
      ("u0", "i0", 4), 
      ("u0", "i2", 5),
      ("u0", "i3", 2),
      ("u1", "i0", 5))
   
  val test2Params: Map[String, String] = Map("viewParam" -> "3", "likeParam" -> "4", "dislikeParam" -> "1", "conversionParam" -> "5",
      "conflictParam" -> "latest")
  val test2ParamsHighest = test2Params + ("conflictParam" -> "highest")
  val test2ParamsLowest = test2Params + ("conflictParam" -> "lowest")
      
  "itemrec.generic DataPreparator with only rate actions, all itypes, conflict=latest" should {
    test(test2AllItypes, test2Params, test2Items, test2U2i, test2RatingsLatest, test2Items)
  }
  
  "itemrec.generic DataPreparator with only rate actions, all itypes, conflict=highest" should {
    test(test2AllItypes, test2ParamsHighest, test2Items, test2U2i, test2RatingsHighest, test2Items)
  }
  
  "itemrec.generic DataPreparator with only rate actions, all itypes, conflict=lowest" should {
    test(test2AllItypes, test2ParamsLowest, test2Items, test2U2i, test2RatingsLowest, test2Items)
  }
  
  "itemrec.generic DataPreparator with only rate actions, some itypes, conflict=highest" should {
    test(test2Itypes_t1t4, test2ParamsHighest, test2Items, test2U2i, test2RatingsHighest_t1t4, test2Items_t1t4)
  }
  
  /**
   * Test 3. Different Actions without conflicts
   */
  val test3AllItypes = List("t1", "t2", "t3", "t4")
  val test3Items = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test3U2i = List(
      (Rate, "u0", "i0", "123450", "4"), 
      (LikeDislike, "u0", "i1", "123457", "1"),
      (LikeDislike, "u0", "i2", "123458", "0"),
      (View, "u0", "i3", "123459", "0"), // NOTE: assume v field won't be missing
      (Rate, "u1", "i0", "123457", "2"),
      (Conversion, "u1", "i1", "123458", "0"))
      
  val test3Ratings = List(      
      ("u0", "i0", 4), 
      ("u0", "i1", 4),
      ("u0", "i2", 2),
      ("u0", "i3", 1),
      ("u1", "i0", 2),
      ("u1", "i1", 5))
  
  val test3Params: Map[String, String] = Map("viewParam" -> "1", "likeParam" -> "4", "dislikeParam" -> "2", "conversionParam" -> "5",
      "conflictParam" -> "latest") 
  
  "itemrec.generic DataPreparator with only all actions, all itypes, no conflict" should {
    test(test3AllItypes, test3Params, test3Items, test3U2i, test3Ratings, test3Items)
  }
    
  /**
   * test 4. Different Actions with conflicts
   */
  val test4Params: Map[String, String] = Map("viewParam" -> "2", "likeParam" -> "5", "dislikeParam" -> "1", "conversionParam" -> "4",
      "conflictParam" -> "latest")
  val test4ParamsLowest: Map[String, String] = test4Params + ("conflictParam" -> "lowest")
      
  val test4AllItypes = List("t1", "t2", "t3", "t4")
  val test4Items = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test4U2i = List(
      (Rate, "u0", "i0", "123448", "3"),
      (View, "u0", "i0", "123449", "4"), // lowest (2)
      (LikeDislike, "u0", "i0", "123451", "1"), // latest, highest (5)
      (Conversion, "u0", "i0", "123450", "1"), 
      
      (Rate, "u0", "i1", "123456", "1"), // lowest
      (Rate, "u0", "i1", "123457", "4"), // highest
      (View, "u0", "i1", "123458", "3"), // latest (2)

      (Conversion, "u0", "i2", "123461", "2"), // latest, highest  (4)
      (Rate, "u0", "i2", "123459", "3"),
      (View, "u0", "i2", "123460", "5"), // lowest
      
      (Rate, "u0", "i3", "123459", "2"),
      (View, "u1", "i0", "123457", "5"), // (2)
      
      (Rate, "u1", "i1", "123458", "5"), // highest
      (Conversion, "u1", "i1", "123459", "4"), // (4)
      (LikeDislike, "u1", "i1", "123460", "0")) // latest, lowest (1)
      
  val test4RatingsLatest = List(
      ("u0", "i0", 5), 
      ("u0", "i1", 2),
      ("u0", "i2", 4),
      ("u0", "i3", 2),
      ("u1", "i0", 2),
      ("u1", "i1", 1))
  
  val test4Itypes_t3 = List("t3")
  val test4Items_t3 = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i3", "t3,t4"))
  val test4RatingsLowest_t3 = List(
      ("u0", "i0", 2), 
      ("u0", "i1", 1),
      ("u0", "i3", 2),
      ("u1", "i0", 2),
      ("u1", "i1", 1))
      
  "itemrec.generic DataPreparator with only all actions, all itypes, and conflicts=latest" should {
    test(test4AllItypes, test4Params, test4Items, test4U2i, test4RatingsLatest, test4Items)
  }
  
  "itemrec.generic DataPreparator with only all actions, some itypes, and conflicts=lowest" should {
    test(test4Itypes_t3, test4ParamsLowest, test4Items, test4U2i, test4RatingsLowest_t3, test4Items_t3)
  }

}
 