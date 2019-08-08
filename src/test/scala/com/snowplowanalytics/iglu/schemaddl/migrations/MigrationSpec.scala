/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.iglu.schemaddl.migrations

import io.circe.literal._
import cats.data._
import com.snowplowanalytics.iglu.core.{SchemaMap, SchemaVer, SelfDescribingSchema, SchemaKey}
import com.snowplowanalytics.iglu.schemaddl.SpecHelpers._
import com.snowplowanalytics.iglu.schemaddl.migrations.Migration.{OrderedSchemas, MigrateFromError}
import org.specs2.Specification

class MigrationSpec extends Specification { def is = s2"""
  Check common Schema migrations
    create correct addition migration from 1-0-0 to 1-0-1 $e1
    create correct addition migrations from 1-0-0 to 1-0-2 $e2
    create correct addition/modification migrations from 1-0-0 to 1-0-2 $e3
    create correct ordered subschemas from 1-0-0 to 1-0-1 $e4
    create correct ordered subschemas from 1-0-0 to 1-0-2 $e5
    create correct ordered subschemas for complex schema $e6
    create correct ordered subschemas for complex schema $e7
    create correct ordered subschemas for complex schema $e8
    create correct migrations when there are schemas with different vendor and name $e9
    return Ior.left all given schemas are single in their model groups $e10
    return Ior.both if there are both single schemas and more than one schemas in their model groups in given schemas $e11
  buildMigrationMatrix function in Migration
    return Ior.left all given schemas are single in their model groups $e12
    return Ior.both if there are both single schemas and more than one schemas in their model groups in given schemas $e13
    return Ior.right if there are only more than one schemas in their model groups in given schemas $e14
  migrateFrom function in Migration
    return error when schemaKey not found in given schemas $e15
    return error when schemaKey is latest state of given schemas $e16
    create migration as expected when schemaKey is initial version of given schemas $e17
    create migration as expected when schemaKey is second version of given schemas $e18
  """

  def e1 = {
    val initial = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          }
        },
        "additionalProperties": false
      }
    """.schema

    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val fromSchema = json"""{"type": ["integer", "null"], "maximum": 4000}""".schema
    val fromPointer = "/properties/bar".jsonPointer

    val migrations = NonEmptyList.of(
      Migration(
        "com.acme",
        "example",
        SchemaVer.Full(1,0,0),
        SchemaVer.Full(1,0,1),
        SchemaDiff(
          List(fromPointer -> fromSchema),
          Set.empty,
          List.empty)))

    val migrationMap = Map(
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)) -> migrations
    )

    Migration.buildMigrationMap(NonEmptyList.of(initialSchema, secondSchema)) must beEqualTo(Ior.right(migrationMap))
  }

  def e2 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          },
          "baz": {
            "type": "array"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)), third)

    val migrations1 = NonEmptyList.of(
      Migration(
        "com.acme",
        "example",
        SchemaVer.Full(1,0,0),
        SchemaVer.Full(1,0,1),
        SchemaDiff(
          List("/properties/bar".jsonPointer -> json"""{"type": ["integer", "null"], "maximum": 4000}""".schema),
          Set.empty,
          List.empty)),
      Migration(
        "com.acme",
        "example",
        SchemaVer.Full(1,0,0),
        SchemaVer.Full(1,0,2),
        SchemaDiff(
          List(
            "/properties/bar".jsonPointer -> json"""{"type": ["integer", "null"], "maximum": 4000}""".schema,
            "/properties/baz".jsonPointer -> json"""{"type": ["array", "null"]}""".schema),
          Set.empty,
          List.empty)))

    val migrations2 = NonEmptyList.of(
      Migration(
        "com.acme",
        "example",
        SchemaVer.Full(1,0,1),
        SchemaVer.Full(1,0,2),
        SchemaDiff(
          List("/properties/baz".jsonPointer -> json"""{"type": ["array", "null"]}""".schema),
          Set.empty,
          List.empty))
    )

    val migrationMap = Map(
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)) -> migrations1,
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)) -> migrations2
    )

    Migration.buildMigrationMap(NonEmptyList.of(initialSchema, secondSchema, thirdSchema)) must beEqualTo(Ior.right(migrationMap))
  }

  def e3 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val migrations1 = NonEmptyList.of(
      Migration(
        "com.acme",
        "example",
        SchemaVer.Full(1,0,0),
        SchemaVer.Full(1,0,1),
        SchemaDiff(
          List("/properties/bar".jsonPointer -> json"""{"type": ["integer", "null"], "maximum": 4000}""".schema),
          Set(
            SchemaDiff.Modified(
              "/properties/foo".jsonPointer,
              json"""{"type":["string","null"]}""".schema,
              json"""{"type":["string","null"],"maxLength": 20}""".schema)
          ),
          List.empty)))


    val migrationMap = Map(
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)) -> migrations1
    )

    Migration.buildMigrationMap(NonEmptyList.of(initialSchema, secondSchema)) must beEqualTo(Ior.right(migrationMap))
  }

  def e4 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "a_field": {
              "type": "integer"
            },
            "b_field": {
              "type": "integer"
            }
          },
          "required": ["b_field"],
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)


    val schemas = NonEmptyList.of(initialSchema, secondSchema)
    val orderedSubSchemasMap = Migration.buildOrderedSubSchemasMap(schemas)

    val expected = Map(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)) -> List("foo", "b_field", "a_field"))

    val res = extractOrder(orderedSubSchemasMap)

    res must beEqualTo(expected)
  }

  def e5 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            },
            "aField": {
              "type": "integer"
            },
            "cField": {
              "type": "integer"
            },
            "dField": {
              "type": "string"
            }
          },
          "required": ["bar", "cField"],
          "additionalProperties": false
        }
      """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)), third)


    val schemas = NonEmptyList.of(initialSchema, secondSchema, thirdSchema)
    val orderedSubSchemasMap = Migration.buildOrderedSubSchemasMap(schemas)

    val expected = Map(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)) -> List("foo", "bar", "c_field", "a_field", "d_field"))

    val res = extractOrder(orderedSubSchemasMap)

    res must beEqualTo(expected)
  }

  def e6 = {
    val initial = json"""
      {
      	"type":"object",
      	"properties":{
      		"functionName":{
      			"type":"string"
      		},
      		"logStreamName":{
      			"type":"string"
      		},
      		"awsRequestId":{
      			"type":"string"
      		},
      		"remainingTimeMillis":{
      			"type":"integer",
      			"minimum":0
      		},
      		"logGroupName":{
      			"type":"string"
      		},
      		"memoryLimitInMB":{
      			"type":"integer",
      			"minimum":0
      		},
      		"clientContext":{
      			"type":"object",
      			"properties":{
      				"client":{
      					"type":"object",
      					"properties":{
      						"appTitle":{
      							"type":"string"
      						},
      						"appVersionName":{
      							"type":"string"
      						},
      						"appVersionCode":{
      							"type":"string"
      						},
      						"appPackageName":{
      							"type":"string"
      						}
      					},
      					"additionalProperties":false
      				},
      				"custom":{
      					"type":"object",
      					"patternProperties":{
      						".*":{
      							"type":"string"
      						}
      					}
      				},
      				"environment":{
      					"type":"object",
      					"patternProperties":{
      						".*":{
      							"type":"string"
      						}
      					}
      				}
      			},
      			"additionalProperties":false
      		},
      		"identity":{
      			"type":"object",
      			"properties":{
      				"identityId":{
      					"type":"string"
      				},
      				"identityPoolId":{
      					"type":"string"
      				}
      			},
      			"additionalProperties":false
      		}
      	},
      	"additionalProperties":false
      }
    """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.amazon.aws.lambda", "java_context", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val schemas = NonEmptyList.of(initialSchema)
    val orderedSubSchemasMap = Migration.buildOrderedSubSchemasMap(schemas)

    val expected = Map(
      SchemaMap("com.amazon.aws.lambda", "java_context", "jsonschema", SchemaVer.Full(1,0,0)) ->
        List(
          "aws_request_id",
          "client_context.client.app_package_name",
          "client_context.client.app_title",
          "client_context.client.app_version_code",
          "client_context.client.app_version_name",
          "client_context.custom",
          "client_context.environment",
          "function_name",
          "identity.identity_id",
          "identity.identity_pool_id",
          "log_group_name",
          "log_stream_name",
          "memory_limit_in_mb",
          "remaining_time_millis"
        )
    )

    val res = extractOrder(orderedSubSchemasMap)

    res must beEqualTo(expected)
  }

  def e7 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "a_field": {
              "type": "object",
              "properties": {
                "b_field": {
                  "type": "string"
                },
                "c_field": {
                  "type": "object",
                  "properties": {
                    "d_field": {
                      "type": "string"
                    },
                    "e_field": {
                      "type": "string"
                    }
                  }
                },
                "d_field": {
                  "type": "object"
                }
              },
              "required": ["d_field"]
            },
            "b_field": {
              "type": "integer"
            },
            "c_field": {
              "type": "integer"
            },
            "d_field": {
              "type": "object",
              "properties": {
                "e_field": {
                  "type": "string"
                },
                "f_field": {
                  "type": "string"
                }
              }
            }
          },
          "required": ["a_field"],
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val schemas = NonEmptyList.of(initialSchema, secondSchema)
    val orderedSubSchemasMap = Migration.buildOrderedSubSchemasMap(schemas)

    val expected = Map(
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)) ->
        List(
          "foo",
          "a_field.d_field",
          "a_field.b_field",
          "a_field.c_field.d_field",
          "a_field.c_field.e_field",
          "b_field",
          "c_field",
          "d_field.e_field",
          "d_field.f_field"
        )
    )

    val res = extractOrder(orderedSubSchemasMap)

    res must beEqualTo(expected)
  }

  def e8 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "a_field": {
              "type": "object",
              "properties": {
                "b_field": {
                  "type": "string"
                },
                "c_field": {
                  "type": "object",
                  "properties": {
                    "d_field": {
                      "type": "string"
                    },
                    "e_field": {
                      "type": "string"
                    }
                  }
                },
                "d_field": {
                  "type": "object"
                }
              },
              "required": ["d_field"]
            },
            "b_field": {
              "type": "integer"
            },
            "c_field": {
              "type": "integer"
            },
            "d_field": {
              "type": "object",
              "properties": {
                "e_field": {
                  "type": "string"
                },
                "f_field": {
                  "type": "string"
                }
              }
            }
          },
          "required": ["a_field"],
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string",
              "maxLength": 20
            },
            "a_field": {
              "type": "object",
              "properties": {
                "b_field": {
                  "type": "string"
                },
                "c_field": {
                  "type": "object",
                  "properties": {
                    "e_field": {
                      "type": "string"
                    }
                  }
                },
                "d_field": {
                  "type": "object"
                }
              },
              "required": ["d_field"]
            },
            "d_field": {
              "type": "object",
              "properties": {
                "f_field": {
                  "type": "string"
                }
              }
            },
            "e_field": {
              "type": "object",
              "properties": {
                "f_field": {
                  "type": "string"
                },
                "g_field": {
                  "type": "string"
                }
              },
              "required": ["g_field"]
            },
            "f_field": {
              "type": "string"
            },
            "g_field": {
              "type": "string"
            }
          },
          "required": ["a_field", "f_field", "e_field"],
          "additionalProperties": false
        }
      """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,1,0)), third)

    val schemas = NonEmptyList.of(initialSchema, secondSchema, thirdSchema)
    val orderedSubSchemasMap = Migration.buildOrderedSubSchemasMap(schemas)

    val expected = Map(
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,1,0)) ->
        List(
          "foo",
          "a_field.d_field",
          "a_field.b_field",
          "a_field.c_field.e_field",
          "d_field.f_field",
          "e_field.g_field",
          "f_field",
          "e_field.f_field",
          "g_field"
        )
    )

    val res = extractOrder(orderedSubSchemasMap)

    res must beEqualTo(expected)
  }

  def e9 = {
    val initial = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val second = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          }
        },
        "additionalProperties": false
      }
    """.schema

    val schema11 = SchemaMap("com.acme", "example1", "jsonschema", SchemaVer.Full(1,0,0))
    val schema12 = SchemaMap("com.acme", "example1", "jsonschema", SchemaVer.Full(1,0,1))

    val schema21 = SchemaMap("com.acme", "example2", "jsonschema", SchemaVer.Full(1,0,0))
    val schema22 = SchemaMap("com.acme", "example2", "jsonschema", SchemaVer.Full(1,0,1))

    val schema31 = SchemaMap("com.acme", "example3", "jsonschema", SchemaVer.Full(1,0,0))
    val schema32 = SchemaMap("com.acme", "example3", "jsonschema", SchemaVer.Full(1,0,1))

    val schemas = NonEmptyList.of(
      SelfDescribingSchema(schema11, initial),
      SelfDescribingSchema(schema12, second),
      SelfDescribingSchema(schema21, initial),
      SelfDescribingSchema(schema22, second),
      SelfDescribingSchema(schema31, initial),
      SelfDescribingSchema(schema32, second)
    )
    val migrationMap = Migration.buildMigrationMap(schemas)

    migrationMap.right.get.keySet must beEqualTo(Set(schema11, schema21, schema31))
  }

  def e10 = {
    val schemaData = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema

    val schema1 = SchemaMap("com.acme", "example1", "jsonschema", SchemaVer.Full(1,0,0))
    val schema2 = SchemaMap("com.acme", "example2", "jsonschema", SchemaVer.Full(1,0,0))

    val schemas = NonEmptyList.of(
      SelfDescribingSchema(schema1, schemaData),
      SelfDescribingSchema(schema2, schemaData)
    )

    val res = Migration.buildMigrationMap(schemas)

    (res.right must beNone) and (res.left must beSome(schemas))
  }

  def e11 = {
    val initial = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          }
        },
        "additionalProperties": false
      }
    """.schema

    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)


    val singleSchemaInGroup1 = SelfDescribingSchema(SchemaMap("com.acme", "example2", "jsonschema", SchemaVer.Full(1,0,0)), second)
    val singleSchemaInGroup2 = SelfDescribingSchema(SchemaMap("com.acme", "example3", "jsonschema", SchemaVer.Full(1,0,0)), second)


    val fromSchema = json"""{"type": ["integer", "null"], "maximum": 4000}""".schema
    val fromPointer = "/properties/bar".jsonPointer

    val migrations = NonEmptyList.of(
      Migration(
        "com.acme",
        "example",
        SchemaVer.Full(1,0,0),
        SchemaVer.Full(1,0,1),
        SchemaDiff(
          List(fromPointer -> fromSchema),
          Set.empty,
          List.empty)))

    val migrationMap = Map(
      SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)) -> migrations
    )

    val res = Migration.buildMigrationMap(NonEmptyList.of(initialSchema, secondSchema, singleSchemaInGroup1, singleSchemaInGroup2))
    res must beEqualTo(Ior.both(NonEmptyList.of(singleSchemaInGroup1, singleSchemaInGroup2), migrationMap))
  }

  def e12 = {
    val schemaData = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema

    val schema1 = SchemaMap("com.acme", "example1", "jsonschema", SchemaVer.Full(1,0,0))
    val schema2 = SchemaMap("com.acme", "example2", "jsonschema", SchemaVer.Full(1,0,0))

    val schemas = NonEmptyList.of(
      SelfDescribingSchema(schema1, schemaData),
      SelfDescribingSchema(schema2, schemaData)
    )

    val res = Migration.buildMigrationMatrix(schemas)

    (res.right must beNone) and (res.left must beSome(schemas))
  }

  def e13 = {
    val initial = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          }
        },
        "additionalProperties": false
      }
    """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)


    val singleSchemaInGroup1 = SelfDescribingSchema(SchemaMap("com.acme", "example2", "jsonschema", SchemaVer.Full(1,0,0)), second)
    val singleSchemaInGroup2 = SelfDescribingSchema(SchemaMap("com.acme", "example3", "jsonschema", SchemaVer.Full(1,0,0)), second)

    val expected = Ior.both(
      NonEmptyList.of(singleSchemaInGroup1, singleSchemaInGroup2),
      NonEmptyList.of(
        OrderedSchemas(
          NonEmptyList.of(initialSchema, secondSchema)
        )
      )
    )
    val res = Migration.buildMigrationMatrix(NonEmptyList.of(initialSchema, secondSchema, singleSchemaInGroup1, singleSchemaInGroup2))

    res must beEqualTo(expected)
  }

  def e14 = {
    val initial = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          }
        },
        "additionalProperties": false
      }
    """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val expected = Ior.right(
      NonEmptyList.of(
        OrderedSchemas(
          NonEmptyList.of(initialSchema, secondSchema)
        )
      )
    )
    val res = Migration.buildMigrationMatrix(NonEmptyList.of(initialSchema, secondSchema))

    res must beEqualTo(expected)
  }


  def e15 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          },
          "baz": {
            "type": "array"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)), third)

    val nonExistingSchemaKey = SchemaKey("com.acme", "non-existing", "jsonschema", SchemaVer.Full(1,0,0))
    val orderedSchemas = OrderedSchemas(NonEmptyList.of(initialSchema, secondSchema, thirdSchema))

    val res = Migration.migrateFrom(nonExistingSchemaKey, orderedSchemas)

    res must beLeft(MigrateFromError.SchemaKeyNotFoundInSchemas)
  }

  def e16 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          },
          "baz": {
            "type": "array"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)), third)

    val orderedSchemas = OrderedSchemas(NonEmptyList.of(initialSchema, secondSchema, thirdSchema))

    val res = Migration.migrateFrom(thirdSchema.self.schemaKey, orderedSchemas)

    res must beLeft(MigrateFromError.SchemaInLatestState)
  }

  def e17 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          },
          "baz": {
            "type": "array"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)), third)

    val migration = Migration(
      "com.acme",
      "example",
      SchemaVer.Full(1,0,0),
      SchemaVer.Full(1,0,2),
      SchemaDiff(
        List(
          "/properties/bar".jsonPointer -> json"""{"type": ["integer", "null"], "maximum": 4000}""".schema,
          "/properties/baz".jsonPointer -> json"""{"type": ["array", "null"]}""".schema),
        Set.empty,
        List.empty)
    )

    val orderedSchemas = OrderedSchemas(NonEmptyList.of(initialSchema, secondSchema, thirdSchema))

    val res = Migration.migrateFrom(initialSchema.self.schemaKey, orderedSchemas)

    res must beRight(migration)
  }

  def e18 = {
    val initial = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      """.schema
    val initialSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,0)), initial)

    val second = json"""
        {
          "type": "object",
          "properties": {
            "foo": {
              "type": "string"
            },
            "bar": {
              "type": "integer",
              "maximum": 4000
            }
          },
          "additionalProperties": false
        }
      """.schema
    val secondSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,1)), second)

    val third = json"""
      {
        "type": "object",
        "properties": {
          "foo": {
            "type": "string"
          },
          "bar": {
            "type": "integer",
            "maximum": 4000
          },
          "baz": {
            "type": "array"
          }
        },
        "additionalProperties": false
      }
    """.schema
    val thirdSchema = SelfDescribingSchema(SchemaMap("com.acme", "example", "jsonschema", SchemaVer.Full(1,0,2)), third)

    val migration = Migration(
      "com.acme",
      "example",
      SchemaVer.Full(1,0,1),
      SchemaVer.Full(1,0,2),
      SchemaDiff(
        List("/properties/baz".jsonPointer -> json"""{"type": ["array", "null"]}""".schema),
        Set.empty,
        List.empty)
    )

    val orderedSchemas = OrderedSchemas(NonEmptyList.of(initialSchema, secondSchema, thirdSchema))

    val res = Migration.migrateFrom(secondSchema.self.schemaKey, orderedSchemas)

    res must beRight(migration)
  }
}
