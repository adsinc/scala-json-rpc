package io.github.shogowada.scala.jsonrpc.serializers

import upickle.Js

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JSONRPCPickler extends upickle.AttributeTagged {
  override implicit def OptionW[T: Writer]: Writer[Option[T]] = {
    Writer {
      case None => Js.Null
      case Some(value) => implicitly[Writer[T]].write(value)
    }
  }

  override implicit def OptionR[T: Reader]: Reader[Option[T]] = {
    Reader {
      case Js.Null => None
      case value: Js.Value => Some(implicitly[Reader[T]].read(value))
    }
  }

  implicit def IdW: Writer[Either[String, BigDecimal]] = {
    Writer[Either[String, BigDecimal]] {
      case Left(value) => writeJs(value)
      case Right(value) => writeJs(value)
    }
  }

  implicit def IdR: Reader[Either[String, BigDecimal]] = {
    Reader[Either[String, BigDecimal]] {
      case value: Js.Str => Left(readJs[String](value))
      case value: Js.Num => Right(readJs[BigDecimal](value))
    }
  }
}

class UpickleJSONSerializer extends JSONSerializer {
  override def serialize[T](value: T): Option[String] = macro UpickleJSONSerializerMacro.serialize[T]

  override def deserialize[T](json: String): Option[T] = macro UpickleJSONSerializerMacro.deserialize[T]
}

object UpickleJSONSerializer {
  def apply() = new UpickleJSONSerializer
}


object UpickleJSONSerializerMacro {
  def serialize[T](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Option[String]] = {
    import c.universe._

    c.Expr[Option[String]](
      q"""
          scala.util.Try(io.github.shogowada.scala.jsonrpc.serializers.JSONRPCPickler.write($value)).toOption
          """
    )
  }

  def deserialize[T: c.WeakTypeTag](c: blackbox.Context)(json: c.Expr[String]): c.Expr[Option[T]] = {
    import c.universe._

    val deserializeType = weakTypeOf[T]

    c.Expr[Option[T]](
      q"""
          scala.util.Try(io.github.shogowada.scala.jsonrpc.serializers.JSONRPCPickler.read[$deserializeType]($json)).toOption
          """
    )
  }
}
