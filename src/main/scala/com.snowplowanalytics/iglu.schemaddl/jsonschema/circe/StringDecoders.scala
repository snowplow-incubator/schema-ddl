/*
 * Copyright (c) 2016-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.iglu.schemaddl.jsonschema
package circe

import java.net.URI

import io.circe._

import cats.implicits._

import properties.StringProperty._

trait StringDecoders {
  implicit val formatDecoder: Decoder[Format] =
    Decoder[String].map(Format.fromString)

  implicit val minLengthDecoder: Decoder[MinLength] =
    Decoder[BigInt].map(MinLength.apply)

  implicit val maxLengthDecoder: Decoder[MaxLength] =
    Decoder[BigInt].map(MaxLength.apply)

  implicit val patterDecoder: Decoder[Pattern] =
    Decoder[String].map(Pattern.apply)

  implicit val stringDecodersJavaUriDecoder: Decoder[URI] =
    Decoder.instance { cursor =>
      cursor.as[String].flatMap { str =>
        Either.catchNonFatal(URI.create(str))
          .leftMap(e => DecodingFailure(e.getMessage, cursor.history))
      }
    }

  implicit val schemaUriDecoder: Decoder[SchemaUri] =
    Decoder[URI].map(SchemaUri.apply)
}
