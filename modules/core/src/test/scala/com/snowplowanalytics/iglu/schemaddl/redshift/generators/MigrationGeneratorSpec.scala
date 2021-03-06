/*
 * Copyright (c) 2012-2021 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.iglu.schemaddl.redshift
package generators

import com.snowplowanalytics.iglu.schemaddl.migrations.{Migration, SchemaDiff}
import io.circe.literal._

// specs2
import org.specs2.Specification

// Iglu
import com.snowplowanalytics.iglu.core.SchemaVer

// This library
import com.snowplowanalytics.iglu.schemaddl.SpecHelpers._
import com.snowplowanalytics.iglu.schemaddl.jsonschema.{ Pointer, Schema }

class MigrationGeneratorSpec extends Specification { def is = s2"""
  Check Redshift migrations generation
    generate addition migration with one new column $e1
    generate addition migration without visible changes $e2
    generate addition migration with three new columns $e3
  """

  val emptyModified = Set.empty[SchemaDiff.Modified]
  val emptySubschemas = List.empty[(Pointer.SchemaPointer, Schema)]

  def e1 = {
    val diff = SchemaDiff(List("status".jsonPointer -> json"""{"type": ["string", "null"]}""".schema), emptyModified, emptySubschemas)
    val schemaMigration = Migration("com.acme", "launch_missles", SchemaVer.Full(1,0,0), SchemaVer.Full(1,0,1), diff)
    val ddlMigration = MigrationGenerator.generateMigration(schemaMigration, 4096, Some("atomic")).render

    val result =
      """|-- WARNING: only apply this file to your database if the following SQL returns the expected:
         |--
         |-- SELECT pg_catalog.obj_description(c.oid) FROM pg_catalog.pg_class c WHERE c.relname = 'com_acme_launch_missles_1';
         |--  obj_description
         |-- -----------------
         |--  iglu:com.acme/launch_missles/jsonschema/1-0-0
         |--  (1 row)
         |
         |BEGIN TRANSACTION;
         |
         |  ALTER TABLE atomic.com_acme_launch_missles_1
         |    ADD COLUMN "status" VARCHAR(4096) ENCODE ZSTD;
         |
         |  COMMENT ON TABLE atomic.com_acme_launch_missles_1 IS 'iglu:com.acme/launch_missles/jsonschema/1-0-1';
         |
         |END TRANSACTION;""".stripMargin

    ddlMigration must beEqualTo(result)
  }

  def e2 = {
    val diff = SchemaDiff.empty
    val schemaMigration = Migration("com.acme", "launch_missles", SchemaVer.Full(2,0,0), SchemaVer.Full(2,0,1), diff)
    val ddlMigration = MigrationGenerator.generateMigration(schemaMigration, 4096, Some("atomic")).render

    val result =
      """|-- WARNING: only apply this file to your database if the following SQL returns the expected:
         |--
         |-- SELECT pg_catalog.obj_description(c.oid) FROM pg_catalog.pg_class c WHERE c.relname = 'com_acme_launch_missles_2';
         |--  obj_description
         |-- -----------------
         |--  iglu:com.acme/launch_missles/jsonschema/2-0-0
         |--  (1 row)
         |
         |BEGIN TRANSACTION;
         |
         |-- NO ADDED COLUMNS CAN BE EXPRESSED IN SQL MIGRATION
         |
         |  COMMENT ON TABLE atomic.com_acme_launch_missles_2 IS 'iglu:com.acme/launch_missles/jsonschema/2-0-1';
         |
         |END TRANSACTION;""".stripMargin

    ddlMigration must beEqualTo(result)
  }

  def e3 = {
    val newProps = List(
      "/status".jsonPointer -> json"""{"type": ["string", "null"]}""".schema,
      "/launch_time".jsonPointer -> json"""{"type": ["string", "null"], "format": "date-time"}""".schema,
      "/latitude".jsonPointer -> json"""{"type": "number", "minimum": -90, "maximum": 90}""".schema,
      "/longitude".jsonPointer ->json"""{"type": "number", "minimum": -180, "maximum": 180}""".schema)

    val diff = SchemaDiff(newProps, emptyModified, emptySubschemas)
    val schemaMigration = Migration("com.acme", "launch_missles", SchemaVer.Full(1,0,2), SchemaVer.Full(1,0,3), diff)
    val ddlMigration = MigrationGenerator.generateMigration(schemaMigration, 4096, Some("atomic")).render

    // TODO: NOT NULL columns should be first
    val result =
      """|-- WARNING: only apply this file to your database if the following SQL returns the expected:
         |--
         |-- SELECT pg_catalog.obj_description(c.oid) FROM pg_catalog.pg_class c WHERE c.relname = 'com_acme_launch_missles_1';
         |--  obj_description
         |-- -----------------
         |--  iglu:com.acme/launch_missles/jsonschema/1-0-2
         |--  (1 row)
         |
         |BEGIN TRANSACTION;
         |
         |  ALTER TABLE atomic.com_acme_launch_missles_1
         |    ADD COLUMN "status" VARCHAR(4096) ENCODE ZSTD;
         |  ALTER TABLE atomic.com_acme_launch_missles_1
         |    ADD COLUMN "launch_time" TIMESTAMP ENCODE ZSTD;
         |  ALTER TABLE atomic.com_acme_launch_missles_1
         |    ADD COLUMN "latitude" DOUBLE PRECISION NOT NULL ENCODE RAW;
         |  ALTER TABLE atomic.com_acme_launch_missles_1
         |    ADD COLUMN "longitude" DOUBLE PRECISION NOT NULL ENCODE RAW;
         |
         |  COMMENT ON TABLE atomic.com_acme_launch_missles_1 IS 'iglu:com.acme/launch_missles/jsonschema/1-0-3';
         |
         |END TRANSACTION;""".stripMargin

    ddlMigration must beEqualTo(result)
  }
}
