package molly.medeia

import cats.data.EitherNec
import cats.effect.kernel.Async
import cats.syntax.functor.*
import cats.syntax.monadError.*
import medeia.codec.BsonDocumentCodec
import medeia.decoder.BsonDecoderError
import molly.core.MollyCodec
import org.bson.BsonDocument

/** An implementation of [[molly.core.MollyCodec]] that uses to [[https://github.com/medeia/medeia Medeia]] for decoding
  * and encoding.
  */
object codec:

  given instance[F[_], A: BsonDocumentCodec](using f: Async[F]): MollyCodec[F, A] =
    new MollyCodec[F, A]:
      override def decode(doc: BsonDocument): F[A] =
        f.delay(BsonDocumentCodec[A].decode(doc)).map(summerizeErrors).rethrow

      override def encode(obj: A): F[BsonDocument] = f.delay(BsonDocumentCodec[A].encode(obj))

      private def summerizeErrors(errorChain: EitherNec[BsonDecoderError, A]): Either[BsonDecoderError, A] =
        errorChain.left.map(_.head)
