package com.github.dsebban.http4s.crud.routes.mongo
import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID }
object types {

  case class WithId[A](id: String, data: A)
  object WithId {
    implicit def withIdBSONWriter[T](implicit dataBSONWriter: BSONDocumentWriter[T]) =
      new BSONDocumentWriter[WithId[T]] {
        override def write(t: WithId[T]): BSONDocument =
          writeWithId(t, dataBSONWriter)
      }

    implicit def withIdBSONReader[T](implicit dataBSONReader: BSONDocumentReader[T]) =
      new BSONDocumentReader[WithId[T]] {
        override def read(bson: BSONDocument): WithId[T] =
          readWithId(bson, dataBSONReader)
      }
    private def writeWithId[T](t: WithId[T], dataBSONWriter: BSONDocumentWriter[T]): BSONDocument = {
      val data = dataBSONWriter.write(t.data)
      //TODO dor: avoid using get here and carry the option outside.
      BSONDocument("_id" -> BSONObjectID.parse(t.id).get) ++ data
    }

    @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
    private def readWithId[T](
        bson: BSONDocument,
        dataBSONReader: BSONDocumentReader[T]
    ): WithId[T] = {
      val id   = bson.getAsTry[BSONObjectID]("_id").get.stringify
      val data = dataBSONReader.read(bson)
      WithId(id, data)
    }
  }
}
