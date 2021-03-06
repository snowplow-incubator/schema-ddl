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

import cats.data.NonEmptyList

import io.circe.literal._

// specs2
import org.specs2.Specification

import com.snowplowanalytics.iglu.core.{SchemaMap, SchemaVer}
import com.snowplowanalytics.iglu.schemaddl.SpecHelpers._
import com.snowplowanalytics.iglu.schemaddl.migrations.FlatSchema

class DdlFileSpec extends Specification { def is = s2"""
  Check DDL File specification
    render correct table definition $e1
    render correct table definition when given schema contains oneOf $e2
    render correct table definition when given schema contains oneOf $e3
    render correct table definition when given schema contains union type $e4
    render correct table definition with table constraints $e5
  """

  def e1 = {
    val header = CommentBlock(Vector(
      "AUTO-GENERATED BY schema-ddl DO NOT EDIT",
      "Generator: schema-ddl 0.2.0",
      "Generated: 2016-03-31 15:52"
    ))
    val schemaCreate = CreateSchema("atomic")

    val createTable = CreateTable(
     "launch_missles_1",
      List(
        Column("status", RedshiftVarchar(64), Set(DistKey), Set(Nullability(NotNull))),
        Column("missionName", RedshiftVarchar(128), Set(), Set(Nullability(NotNull))),
        Column("geo_longitude", RedshiftDouble, Set(), Set()),
        Column("geo_latitude", RedshiftDouble, Set(), Set()),
        Column("rocket.model", RedshiftInteger, Set(), Set(Nullability(NotNull))),
        Column("rocket.series", RedshiftInteger, Set(), Set(Nullability(Null)))
      )
    )
    val commentOn = DdlGenerator.getTableComment(
      "launch_missles_1",
      Some("atomic"),
      SchemaMap("com.acme", "event", "jsonschema", SchemaVer.Full(1,2,1))
    )

    // no formatters
    val ddl = DdlFile(List(header, schemaCreate, createTable, commentOn)).render(Nil)

    // ordering happens in DdlGenerator.getTableDdl function
    ddl must beEqualTo(
      """|-- AUTO-GENERATED BY schema-ddl DO NOT EDIT
         |-- Generator: schema-ddl 0.2.0
         |-- Generated: 2016-03-31 15:52
         |CREATE SCHEMA IF NOT EXISTS atomic;
         |CREATE TABLE IF NOT EXISTS launch_missles_1 (
         |    "status"        VARCHAR(64)      DISTKEY NOT NULL,
         |    "missionName"   VARCHAR(128)             NOT NULL,
         |    "geo_longitude" DOUBLE PRECISION,
         |    "geo_latitude"  DOUBLE PRECISION,
         |    "rocket.model"  INT                      NOT NULL,
         |    "rocket.series" INT                      NULL
         |);
         |COMMENT ON TABLE atomic.launch_missles_1 IS 'iglu:com.acme/event/jsonschema/1-2-1';""".stripMargin)
  }

  def e2 = {
    val json = json"""
      {
        "type": "object",
        "properties": {
          "union": {
            "oneOf": [
              {
                "type": "object",
                "properties": {
                  "object_without_properties": { "type": "object" }
                }
              },
              {
                "type": "string"
              }
            ]
          }
        },
        "additionalProperties": false
      }
    """.schema
    val expected =
      """CREATE TABLE IF NOT EXISTS atomic.table_name (
        |    "schema_vendor"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_name"    VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_format"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_version" VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "root_id"        CHAR(36)      ENCODE RAW  NOT NULL,
        |    "root_tstamp"    TIMESTAMP     ENCODE ZSTD NOT NULL,
        |    "ref_root"       VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "ref_tree"       VARCHAR(1500) ENCODE ZSTD NOT NULL,
        |    "ref_parent"     VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "union"          VARCHAR(1024) ENCODE ZSTD,
        |    FOREIGN KEY (root_id) REFERENCES atomic.events(event_id)
        |)
        |DISTSTYLE KEY
        |DISTKEY (root_id)
        |SORTKEY (root_tstamp);""".stripMargin

    val flatSchema = FlatSchema.build(json)
    val orderedSubSchemas = FlatSchema.postProcess(flatSchema.subschemas)
    val schemaCreate = DdlGenerator.generateTableDdl(orderedSubSchemas, "table_name", None, 1024, false)
    val ddl = DdlFile(List(schemaCreate)).render(Nil)
    ddl must beEqualTo(expected)
  }

  def e3 = {
    val json = json"""
      {
        "type": "object",
        "properties": {
          "union": {
            "oneOf": [
              {
                "type": "integer"
              },
              {
                "type": "string"
              }
            ]
          }
        },
        "additionalProperties": false
      }
    """.schema
    val expected =
      """CREATE TABLE IF NOT EXISTS atomic.table_name (
        |    "schema_vendor"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_name"    VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_format"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_version" VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "root_id"        CHAR(36)      ENCODE RAW  NOT NULL,
        |    "root_tstamp"    TIMESTAMP     ENCODE ZSTD NOT NULL,
        |    "ref_root"       VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "ref_tree"       VARCHAR(1500) ENCODE ZSTD NOT NULL,
        |    "ref_parent"     VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "union"          VARCHAR(1024) ENCODE ZSTD,
        |    FOREIGN KEY (root_id) REFERENCES atomic.events(event_id)
        |)
        |DISTSTYLE KEY
        |DISTKEY (root_id)
        |SORTKEY (root_tstamp);""".stripMargin

    val flatSchema = FlatSchema.build(json)
    val orderedSubSchemas = FlatSchema.postProcess(flatSchema.subschemas)
    val schemaCreate = DdlGenerator.generateTableDdl(orderedSubSchemas, "table_name", None, 1024, false)
    val ddl = DdlFile(List(schemaCreate)).render(Nil)
    ddl must beEqualTo(expected)
  }

  def e4 = {
    val json = json"""
      {
        "type": "object",
        "properties": {
          "union": {
            "type": ["string", "object"],
            "properties": {
              "one": { "type": "string" },
              "second": { "type": "string" }
            }
          },
          "union2": {
           "type": ["string", "object"],
           "properties": {
             "one": { "type": "string" },
             "second": { "type": "string" }
           }
          }
        },
        "additionalProperties": false
      }
    """.schema
    val expected =
      """CREATE TABLE IF NOT EXISTS atomic.table_name (
        |    "schema_vendor"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_name"    VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_format"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_version" VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "root_id"        CHAR(36)      ENCODE RAW  NOT NULL,
        |    "root_tstamp"    TIMESTAMP     ENCODE ZSTD NOT NULL,
        |    "ref_root"       VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "ref_tree"       VARCHAR(1500) ENCODE ZSTD NOT NULL,
        |    "ref_parent"     VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "union"          VARCHAR(4096) ENCODE ZSTD,
        |    "union2"         VARCHAR(4096) ENCODE ZSTD,
        |    FOREIGN KEY (root_id) REFERENCES atomic.events(event_id)
        |)
        |DISTSTYLE KEY
        |DISTKEY (root_id)
        |SORTKEY (root_tstamp);""".stripMargin

    val flatSchema = FlatSchema.build(json)
    val orderedSubSchemas = FlatSchema.postProcess(flatSchema.subschemas)
    val schemaCreate = DdlGenerator.generateTableDdl(orderedSubSchemas, "table_name", None, 1024, false)
    val ddl = DdlFile(List(schemaCreate)).render(Nil)
    ddl must beEqualTo(expected)
  }

  def e5 = {
    val schemaCreate = CreateTable(
      "atomic.table_name",
      List(
        Column("schema_vendor",RedshiftVarchar(128),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("schema_name",RedshiftVarchar(128),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("schema_format",RedshiftVarchar(128),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("schema_version",RedshiftVarchar(128),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("root_id",RedshiftChar(36),Set(CompressionEncoding(RawEncoding)),Set(Nullability(NotNull))),
        Column("root_tstamp",RedshiftTimestamp,Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("ref_root",RedshiftVarchar(255),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("ref_tree",RedshiftVarchar(1500),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("ref_parent",RedshiftVarchar(255),Set(CompressionEncoding(ZstdEncoding)),Set(Nullability(NotNull))),
        Column("union",ProductType(List("Product type [\"string\",\"object\",\"null\"] encountered in union"),None),Set(CompressionEncoding(ZstdEncoding)),Set()),
        Column("union2",ProductType(List("Product type [\"string\",\"object\",\"null\"] encountered in union2"),None),Set(CompressionEncoding(ZstdEncoding)),Set())
      ),
      Set(
        ForeignKeyTable(NonEmptyList.of("root_id", "root_tstamp"),RefTable("atomic.events",Some("event_id"))),
        PrimaryKeyTable(NonEmptyList.of("root_id", "root_tstamp")),
        UniqueKeyTable(NonEmptyList.of("root_id", "root_tstamp")),
      ),
      Set(Diststyle(Key), DistKeyTable("root_id"), SortKeyTable(None,NonEmptyList.one("root_tstamp")))
    )

    val expected =
      """CREATE TABLE IF NOT EXISTS atomic.table_name (
        |    "schema_vendor"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_name"    VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_format"  VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "schema_version" VARCHAR(128)  ENCODE ZSTD NOT NULL,
        |    "root_id"        CHAR(36)      ENCODE RAW  NOT NULL,
        |    "root_tstamp"    TIMESTAMP     ENCODE ZSTD NOT NULL,
        |    "ref_root"       VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "ref_tree"       VARCHAR(1500) ENCODE ZSTD NOT NULL,
        |    "ref_parent"     VARCHAR(255)  ENCODE ZSTD NOT NULL,
        |    "union"          VARCHAR(4096) ENCODE ZSTD,
        |    "union2"         VARCHAR(4096) ENCODE ZSTD,
        |    FOREIGN KEY (root_id, root_tstamp) REFERENCES atomic.events(event_id)
        |    PRIMARY KEY (root_id, root_tstamp)
        |    UNIQUE (root_id, root_tstamp)
        |)
        |DISTSTYLE KEY
        |DISTKEY (root_id)
        |SORTKEY (root_tstamp);""".stripMargin


    val ddl = DdlFile(List(schemaCreate)).render(Nil)
    ddl must beEqualTo(expected)
  }
}
