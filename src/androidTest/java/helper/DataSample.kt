package helper

const val MOCK_DATA_IMPRESSION = """
    {
        "featureName": "onboarding",
        "keyName": "9oIdSh7h+ACsSTBhZBMzgRHmljmcuZ6OioFinkdf4EtuElENWIYloE",
        "bucketingKey": null,
        "treatment": "on",
        "label": "default rule",
        "time":" 546546546549,
        "changeNumber": 546546546549,
        "previousTime": 498357419865
    }
    """

const val MOCK_DATA_EVENT: String = """
    {
         "key": "CD16FqbutYKb5Mgd4TwAcXFOZsKrVm9QJM5LL1lwHoIn4SmOdOMqH",
         "eventTypeId": "id33",
         "trafficTypeName": "user",
         "value": 123.0,
         "timestamp": 5465465469,
         "properties": ["prop1": 1, "prop2": "dos"],
         "sizeInBytes": 0
    }
    """

const val MOCK_DATA_SPLIT: String = """
    {
    "trafficTypeName":"account",
    "name":"FACUNDO_TEST",
    "trafficAllocation":59,
    "trafficAllocationSeed":-2108186082,
    "seed":-1947050785,
    "status":"ACTIVE",
    "killed":false,
    "defaultTreatment":"off",
    "changeNumber":1506703262916,
    "algo":2,
    "conditions":[
                {
                "conditionType":"WHITELIST",
                "matcherGroup":{
                "combiner":"AND",
                "matchers":[
                            {
                            "keySelector":null,
                            "matcherType":"WHITELIST",
                            "negate":false,
                            "userDefinedSegmentMatcherData":null,
                            "whitelistMatcherData":{
                            "whitelist":[
                                         "nico_test",
                                         "othertest"
                                         ]
                            },
                            "unaryNumericMatcherData":null,
                            "betweenMatcherData":null,
                            "booleanMatcherData":null,
                            "dependencyMatcherData":null,
                            "stringMatcherData":null
                            }
                            ]
                },
                "partitions":[
                              {
                              "treatment":"on",
                              "size":100
                              }
                              ],
                "label":"whitelisted"
                },
                {
                "conditionType":"WHITELIST",
                "matcherGroup":{
                "combiner":"AND",
                "matchers":[
                            {
                            "keySelector":null,
                            "matcherType":"WHITELIST",
                            "negate":false,
                            "userDefinedSegmentMatcherData":null,
                            "whitelistMatcherData":{
                            "whitelist":[
                                         "bla"
                                         ]
                            },
                            "unaryNumericMatcherData":null,
                            "betweenMatcherData":null,
                            "booleanMatcherData":null,
                            "dependencyMatcherData":null,
                            "stringMatcherData":null
                            }
                            ]
                },
                "partitions":[
                              {
                              "treatment":"off",
                              "size":100
                              }
                              ],
                "label":"whitelisted"
                },
                {
                "conditionType":"ROLLOUT",
                "matcherGroup":{
                "combiner":"AND",
                "matchers":[
                            {
                            "keySelector":{
                            "trafficType":"account",
                            "attribute":null
                            },
                            "matcherType":"ALL_KEYS",
                            "negate":false,
                            "userDefinedSegmentMatcherData":null,
                            "whitelistMatcherData":null,
                            "unaryNumericMatcherData":null,
                            "betweenMatcherData":null,
                            "booleanMatcherData":null,
                            "dependencyMatcherData":null,
                            "stringMatcherData":null
                            }
                            ]
                },
                "partitions":[
                              {
                              "treatment":"on",
                              "size":0
                              },
                              {
                              "treatment":"off",
                              "size":100
                              },
                              {
                              "treatment":"visa",
                              "size":0
                              }
                              ],
                "label":"in segment all"
                }
                ]
    }
"""

const val MOCK_DATA_LONG_TEXT = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed dolor arcu, ultrices eget diam ut, tincidunt rhoncus felis. Suspendisse vel libero nec risus tincidunt ultricies. Aenean a vulputate nulla, a viverra enim. Phasellus volutpat magna quam, non ultrices lorem facilisis id. Morbi ultrices est augue. Integer interdum nisi at erat molestie auctor. Donec in diam vel sapien tincidunt luctus."""
