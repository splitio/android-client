package helper

class DataSample {

    public val jsonImpression = """
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

    public val jsonEvent: String = """
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

    public val jsonSplit: String = """
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

    public val veryLongText = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed dolor arcu, ultrices eget diam ut, tincidunt rhoncus felis. Suspendisse vel libero nec risus tincidunt ultricies. Aenean a vulputate nulla, a viverra enim. Phasellus volutpat magna quam, non ultrices lorem facilisis id. Morbi ultrices est augue. Integer interdum nisi at erat molestie auctor. Donec in diam vel sapien tincidunt luctus.

Cras maximus sit amet tortor quis eleifend. Vivamus risus augue, dapibus in accumsan sit amet, pharetra quis elit. Aliquam id sodales diam. Nullam fermentum orci sit amet ipsum molestie, at malesuada magna imperdiet. Quisque fermentum imperdiet sapien at pellentesque. Aliquam vel ornare nisl. Phasellus tempus et libero quis iaculis.

Nam ultricies neque sit amet convallis pellentesque. Praesent vitae odio eu sem fringilla varius et eget purus. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Suspendisse elementum placerat nisi sit amet pretium. Fusce ac arcu urna. Cras feugiat dui vitae nisl feugiat sagittis. Donec blandit lectus vitae massa accumsan, at dictum augue pretium. Sed vel elit justo.

Mauris id velit a massa cursus blandit. Suspendisse rutrum faucibus viverra. In sapien leo, vehicula et orci ac, rhoncus auctor libero. In elementum elit non arcu eleifend, nec tempus tellus efficitur. Donec finibus odio at magna fringilla egestas ac ut leo. Nullam placerat leo lorem, quis suscipit sem dignissim sed. Sed tincidunt pretium nibh vel mattis. Praesent nibh massa, dictum a nulla sed, pretium elementum eros. Suspendisse pretium vestibulum sapien a consectetur. In nec lobortis massa.

Cras blandit iaculis gravida. Nullam tempor tincidunt mauris sed tristique. Fusce finibus malesuada sapien at blandit. Proin ut nisl tincidunt est blandit maximus. Cras sit amet dictum orci. Nullam quis sapien rhoncus, porttitor libero ut, convallis turpis. In hac habitasse platea dictumst. Sed sagittis velit orci, ut pretium nunc hendrerit in. Interdum et malesuada fames ac ante ipsum primis in faucibus. Sed eu imperdiet odio. Integer eget pellentesque justo. Vestibulum sem mi, accumsan nec gravida commodo, pulvinar hendrerit sem. Fusce molestie justo vitae nisl semper, et imperdiet neque varius.

Praesent a eros commodo, fermentum arcu eu, faucibus enim. Donec viverra luctus luctus. Maecenas finibus malesuada nibh non molestie. Nulla sit amet cursus nisl, eu laoreet arcu. Integer pulvinar gravida ultricies. Interdum et malesuada fames ac ante ipsum primis in faucibus. Duis tellus arcu, facilisis a orci nec, luctus pretium elit.

Vestibulum nec odio sed augue hendrerit hendrerit. Ut risus nulla, laoreet vitae lorem nec, tincidunt lobortis orci. Interdum et malesuada fames ac ante ipsum primis in faucibus. Donec ornare sollicitudin rhoncus. Aenean sit amet velit et urna imperdiet volutpat. Nullam tempor a nibh eget laoreet. Nulla facilisi. Vestibulum sed bibendum libero. Aenean molestie ligula a elit scelerisque, id vestibulum diam ornare. Duis eu orci sit amet sapien mattis posuere quis ut quam. Duis volutpat sapien nec elit gravida, sed molestie quam hendrerit. Quisque sed sapien et lorem ultrices rhoncus. Etiam a rhoncus arcu, vel varius eros. Duis blandit quam nec sem vulputate, vitae lobortis est volutpat. Phasellus in ex non risus pulvinar tincidunt.

Donec tempus velit ut libero sollicitudin, quis porta urna hendrerit. Integer urna risus, dapibus at arcu et, efficitur varius nunc. Nulla dapibus felis fringilla felis maximus varius. Sed congue turpis nec dui pretium, in facilisis orci sodales. Morbi ultrices orci sed interdum molestie. Aliquam a arcu pharetra, auctor magna at, scelerisque turpis. Etiam ut consequat leo. Cras malesuada congue nulla, eu molestie tellus scelerisque quis. Nam bibendum commodo magna sed euismod. Nulla at velit iaculis dui porttitor vehicula. Duis vestibulum nibh vel dui pulvinar, non tincidunt neque sodales. In vel augue et nisi placerat commodo vitae quis lectus. Duis a tempor orci. Proin dignissim at magna eu elementum. Pellentesque sem sem, pulvinar quis consectetur ac, consectetur nec ipsum.

Integer viverra libero et eros convallis, vel scelerisque magna feugiat. Fusce eget velit non dui porttitor blandit eget quis diam. Vivamus porta bibendum leo, non vestibulum ante. Aenean at sagittis purus, id congue orci. Phasellus vulputate est at ultricies condimentum. Sed posuere nunc vel posuere luctus. Donec vel sollicitudin ligula. Sed sodales turpis ut elit sodales tempus. Proin eu sem fringilla, dignissim libero ac, placerat magna.

Phasellus fringilla tortor nec hendrerit congue. Etiam a felis id tortor facilisis molestie. Donec convallis velit id risus dictum, sit amet gravida risus condimentum. Praesent in tincidunt odio. Aliquam sed quam mattis, elementum mauris non, tempus neque. Curabitur efficitur erat purus, ac accumsan nisl elementum vitae. Donec imperdiet, nunc vitae maximus commodo, tortor ipsum interdum ligula, ac aliquam mauris lorem sit amet est.

Pellentesque euismod ipsum sit amet magna aliquet, quis tempus lorem vestibulum. Curabitur maximus lectus ipsum, ac luctus turpis dapibus eget. Donec sapien orci, tincidunt non mollis vel, ornare non ipsum. Curabitur diam erat, sagittis id tellus vitae, scelerisque posuere felis. Vivamus vulputate, metus et suscipit rutrum, lacus sapien cursus sem, varius faucibus mi est vitae ex. Vestibulum interdum sed purus et fringilla. Suspendisse potenti. Phasellus vitae ultrices leo, quis pretium lacus. Sed a varius justo, at mattis turpis. Pellentesque sed viverra neque. Aliquam elementum nunc nunc, in aliquet diam interdum non. Nullam quis volutpat ipsum. Quisque turpis leo, fringilla id arcu sed, viverra viverra ligula. Vestibulum mauris sapien, luctus id imperdiet in, tempus non felis. Vestibulum dapibus ut mi et facilisis.

Sed posuere rutrum nibh in maximus. Sed in nisl pulvinar, vehicula eros sed, sollicitudin sem. Curabitur odio felis, efficitur eu mauris id, vestibulum commodo mauris. Ut quis nibh sed magna accumsan bibendum. Suspendisse id iaculis lacus, accumsan tristique eros. Vivamus sed sapien nec erat semper scelerisque. Nam efficitur eros ac turpis mattis, quis tincidunt nunc dignissim. Vestibulum at diam in justo rhoncus commodo. Nullam fermentum posuere augue quis vehicula. Aliquam porttitor dapibus justo ut faucibus. Vivamus rhoncus id urna a venenatis. Phasellus aliquam odio nec sapien gravida, id dapibus urna imperdiet. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce rutrum purus blandit nisl vulputate efficitur. Nulla fermentum est non urna pulvinar bibendum. Etiam accumsan erat ante, sed iaculis tellus rhoncus quis.

Aenean non dolor ac eros euismod facilisis. Maecenas et purus mollis, ultricies quam vel, fringilla erat. Phasellus mollis turpis lacus, at sodales ex aliquam non. Nunc consequat purus sed turpis elementum, nec tincidunt urna maximus. Duis ipsum ipsum, lacinia sed mi ut, suscipit aliquet urna. Praesent dictum ex eros, in iaculis orci bibendum eget. Nulla id felis ac odio sodales bibendum. Proin eget metus augue. Proin efficitur velit mi, non gravida elit semper in. Vivamus egestas turpis nisi, eu placerat nunc maximus at. Nunc lacinia sit amet nisl eu euismod. Etiam in nisl id diam iaculis lobortis. Phasellus massa libero, euismod vitae lobortis et, ullamcorper eu elit. Duis cursus maximus nibh, quis volutpat massa egestas eu. Ut dapibus mollis commodo. Nam quis lectus condimentum sem finibus viverra.

Mauris feugiat viverra sem, nec ullamcorper urna. Aliquam a mollis metus. Cras non turpis quis justo auctor mollis a nec leo. Quisque id consequat lacus, at egestas ante. Praesent vitae ipsum lectus. Aliquam faucibus sollicitudin enim nec consequat. Integer lacinia neque nec accumsan condimentum.

Proin rutrum mauris at lorem euismod congue. Fusce elementum, orci vitae efficitur porttitor, risus magna tincidunt justo, in rhoncus lacus nunc a dui. Sed vitae sagittis libero. Etiam tincidunt quis magna et congue. Nunc feugiat tristique metus, id gravida risus pretium at. Maecenas sem elit, dapibus et nulla eu, vehicula aliquam diam. Aenean non dui nunc. Fusce ultrices odio magna. Cras posuere est turpis, at ultrices nibh dignissim vitae. Pellentesque et lectus imperdiet, aliquet velit ac, aliquet mi. Nulla vehicula congue eleifend. In non augue nec magna consequat vehicula. Nunc gravida vel nibh nec blandit. Vivamus sollicitudin diam ac urna ornare, non pellentesque magna ornare. Nulla ullamcorper elit eu mollis tincidunt.

Morbi non orci vel urna pulvinar mattis. Nunc eget metus vestibulum, luctus nulla vitae, mollis elit. Nulla porta varius ex, tristique vulputate eros pretium vitae. Phasellus pulvinar, felis id sollicitudin sagittis, arcu sapien volutpat elit, at mattis justo tortor nec tortor. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nulla sagittis justo eget risus aliquet sodales. Interdum et malesuada fames ac ante ipsum primis in faucibus. Ut quis faucibus diam, vel congue sapien.

Aenean pharetra vel nisl sit amet porttitor. Nam ullamcorper accumsan quam sit amet commodo. Donec in purus ac neque ullamcorper cursus. Donec dictum vel sapien sed interdum. Sed venenatis, orci sit amet dignissim lacinia, urna eros cursus lectus, a pharetra purus neque eget risus. Vivamus dictum arcu eu augue consectetur, ut scelerisque lacus vulputate. Etiam mattis mi erat.

In fringilla massa non elit ullamcorper, at convallis mi sodales. Donec et scelerisque magna. Ut at metus ligula. Maecenas nisi quam, consequat sed blandit ac, iaculis nec enim. Aliquam sit amet placerat leo. Nulla vitae consequat diam. Aenean odio diam, scelerisque sed sapien id, molestie ultrices diam. Donec condimentum sollicitudin leo, ut gravida dolor lobortis nec. Sed aliquam neque non leo egestas elementum. Aliquam dapibus dolor eget est malesuada, vitae suscipit diam efficitur. Duis ac neque a ligula pharetra consectetur ac vel orci.

Duis bibendum pellentesque magna at rhoncus. Fusce pharetra cursus dolor, tincidunt tincidunt ante porttitor vulputate. Nulla eget congue ipsum, at facilisis massa. Aenean quis orci sodales, mollis leo in, lobortis diam. Suspendisse potenti. Curabitur ullamcorper, libero non auctor malesuada, ipsum dolor convallis dolor, non euismod nulla tellus id nisl. Ut mattis justo nec turpis sodales, nec malesuada velit posuere.

Pellentesque cursus ex dui, a interdum diam porta non. Integer blandit ultrices libero, eu pulvinar ex venenatis et. Duis elementum blandit diam, in pretium ante convallis ac. Ut non lacus elit. Donec quis semper lectus, sed pharetra ligula. Quisque sagittis libero lacinia arcu dapibus, pretium convallis sapien consectetur. Duis sapien dolor, blandit eu mi a, faucibus vulputate quam. Nunc a egestas ligula, vel tempus felis. Sed sed porta ipsum. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. In elementum nunc purus, non ornare ex bibendum sit amet. Integer molestie ultrices laoreet.

Nunc eu risus eget tellus gravida lacinia. Quisque accumsan pulvinar maximus. Vivamus turpis ipsum, fringilla sed sodales eu, egestas ac dui. Fusce cursus sollicitudin nisl ac tempus. Nulla non magna dapibus, imperdiet erat a, porta urna. Donec gravida leo at porta suscipit. In in lacinia sapien. Aenean hendrerit, nibh at faucibus sodales, eros leo aliquet mauris, ut vehicula nisl mauris sed sapien. Ut ullamcorper diam et nibh convallis, in venenatis ipsum dapibus. Suspendisse vestibulum ligula placerat fringilla lobortis. Morbi facilisis nisi in nunc ultrices, tristique eleifend tellus commodo. Aenean urna nunc, maximus in tincidunt eu, auctor ut sem.

Morbi sollicitudin dolor risus, ac scelerisque tortor pellentesque nec. Maecenas porta erat augue, nec feugiat ipsum aliquet vel. In mollis a lacus vel varius. Vestibulum in nisl sollicitudin, cursus turpis in, volutpat ligula. Sed lobortis volutpat felis ut malesuada. Proin lectus sem, aliquam eu sollicitudin sit amet, elementum sed tortor. Suspendisse vel rhoncus elit. Aliquam fermentum augue tristique dui luctus convallis. Phasellus volutpat mauris varius dapibus iaculis. Integer vulputate pretium turpis et fringilla. Cras pretium ac purus eu bibendum. Morbi vehicula eu velit quis efficitur. Curabitur eu nibh felis. In aliquet diam at vehicula aliquet.

Donec ut lacus eget arcu tincidunt vulputate. Fusce lacus diam, consequat vestibulum commodo vel, fermentum id ante. Quisque rhoncus mi et lorem volutpat, nec ornare leo ullamcorper. Maecenas dictum nisi eget dolor hendrerit, sed dictum arcu hendrerit. Curabitur auctor sem non ornare lacinia. Cras lacinia sed massa vel aliquet. Phasellus turpis quam, eleifend in suscipit sagittis, aliquam ac ante. Vivamus ut semper neque. Praesent luctus dictum erat a mattis. Ut ut feugiat nibh. Ut sit amet lectus mi. Mauris ac lacinia tellus. Etiam quam urna, feugiat id eleifend vel, cursus non nibh.

Vestibulum vulputate vel risus vitae accumsan. Cras a tristique dolor, quis posuere justo. Nunc metus urna, blandit eu fringilla a, viverra quis justo. Aenean ac congue elit, a vehicula ex. Duis augue dui, tempor at porttitor vel, vestibulum sit amet ante. Sed pellentesque odio sed mi venenatis finibus. Pellentesque eget aliquet orci. Sed imperdiet, dolor vitae bibendum imperdiet, orci turpis ornare metus, quis scelerisque quam ipsum et leo. Praesent tempus faucibus condimentum. Sed sodales urna ac feugiat convallis. Morbi in gravida turpis, ut rutrum sapien. Curabitur vestibulum nisl eget elit vulputate, in posuere nisl blandit. Cras faucibus mauris ut augue gravida ullamcorper.

In in purus non sapien accumsan congue eget eget sem. In elementum, arcu ac iaculis imperdiet, tellus tellus rutrum leo, ut fermentum mi quam ac mauris. Nam condimentum eros felis, maximus fringilla risus iaculis quis. Phasellus vel efficitur massa. Duis porta, eros sed sollicitudin volutpat, nunc sapien ornare turpis, eu interdum libero ante et augue. Phasellus egestas dapibus imperdiet. Cras tincidunt commodo tempus.

Mauris quis porttitor tortor. Maecenas nec nisl eget magna cursus posuere. Duis consequat finibus neque, in molestie justo. Cras ornare fringilla ipsum quis commodo. Integer at rhoncus orci. Etiam tincidunt ligula consectetur orci gravida elementum ac nec neque. Donec efficitur diam a nulla vestibulum, in rutrum enim pretium. Nam ultrices, ipsum id fermentum rhoncus, dui nisl mattis ipsum, a tempus lorem mi sit amet justo. Interdum et malesuada fames ac ante ipsum primis in faucibus. Duis tincidunt nunc vel sem sollicitudin fermentum. Nunc a convallis quam. Curabitur tincidunt, est id maximus ultrices, quam purus tempus orci, at suscipit purus lacus eget lectus. Praesent vitae consectetur nisl, at congue sem. Duis elit libero, ultricies vitae ultrices molestie, commodo nec nibh.

Aenean nec velit semper, auctor orci nec, accumsan sapien. Etiam sit amet dolor lacus. In accumsan fringilla sapien ut aliquet. Praesent id magna in arcu porta venenatis. Donec consectetur, magna eu dignissim laoreet, felis sapien blandit magna, in luctus augue justo at diam. Vivamus eu sagittis purus, sed hendrerit erat. Etiam fermentum et mi vel suscipit. Nunc tortor urna, molestie vitae massa eget, ornare feugiat mi. Mauris ultrices erat ante, in faucibus ante auctor in. Integer condimentum ipsum id fringilla blandit. Duis eget libero rutrum, viverra libero ac, laoreet magna. Donec est nisl, sodales non orci eu, condimentum consectetur ligula.

Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Sed elementum mi quis justo congue hendrerit. Nulla pharetra vestibulum nisi ut interdum. Curabitur tristique luctus sem, vitae commodo sapien. Suspendisse nisi augue, congue et sapien sed, mattis pharetra nisi. Cras finibus neque in purus molestie, at volutpat libero accumsan. Pellentesque ut porta mi. Nam vel ante ut quam condimentum sodales id sed ligula. Morbi tincidunt lorem a elit posuere, nec sagittis velit dignissim. Morbi sollicitudin, ante eget bibendum imperdiet, quam justo ornare tellus, at rutrum leo libero vitae massa. Nulla sagittis lorem hendrerit neque pharetra, eget tristique orci iaculis. Phasellus posuere imperdiet metus et sagittis. Nunc posuere, lorem ut imperdiet aliquet, mauris ipsum mattis nulla, vitae imperdiet nunc velit a nisl. Suspendisse sed libero pulvinar, pretium urna mollis, aliquam arcu. Integer convallis maximus leo vel interdum.

Nunc aliquet tincidunt massa vel dictum. Pellentesque faucibus placerat luctus. Praesent faucibus arcu augue, eget aliquet mi accumsan a. Aenean dolor diam, malesuada eget libero non, hendrerit mattis metus. Nunc at volutpat dui. Donec et enim eu nisl sodales pretium. Praesent laoreet faucibus cursus. Morbi accumsan viverra velit eu condimentum. Nulla pretium mauris porttitor dolor gravida finibus. Sed vestibulum magna vel est euismod varius.

Quisque ut faucibus tortor. Suspendisse potenti. Morbi laoreet odio eu libero vestibulum, sed pretium elit vestibulum. Proin finibus vulputate urna at fermentum. Nulla a sem mauris. Sed viverra at turpis sed ultrices. Nulla venenatis iaculis tristique. Sed orci ligula, dignissim eget libero id, maximus sollicitudin mauris. Etiam erat augue, molestie imperdiet ligula eget, euismod bibendum tortor. Nulla facilisi. Integer dictum neque metus, et bibendum tellus pellentesque in. Donec dapibus vitae odio eget rhoncus. Duis tempor elementum egestas. Cras laoreet feugiat semper. Mauris et aliquam velit, in aliquet purus.

Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec nec magna in urna pretium scelerisque a sed magna. Pellentesque tristique in tellus vel pulvinar. Fusce molestie arcu in erat suscipit, non consequat ante sollicitudin. Duis et massa dictum, dapibus ligula a, facilisis ligula. Ut sit amet libero sagittis tortor tincidunt vulputate. Ut ornare hendrerit tortor eu dictum. Proin non auctor ante. Curabitur sit amet tortor augue. In sagittis, nibh non accumsan tristique, purus nisl iaculis mi, nec mattis ligula nulla nec orci. Pellentesque metus augue, maximus sed vestibulum vitae, ultrices in nulla. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. In massa velit, porta quis imperdiet finibus, ullamcorper sit amet arcu.

Cras auctor finibus leo, non sodales metus dignissim ut. Aenean sed imperdiet nisl. Morbi ut pulvinar est. Mauris bibendum, tortor et efficitur maximus, ipsum augue accumsan purus, et consectetur tellus arcu a justo. Duis ut augue sed erat ullamcorper efficitur eget ac arcu. Donec sagittis ipsum tellus, at consectetur nibh fermentum eget. Nulla ac dolor metus. Curabitur rutrum dapibus nibh, quis hendrerit quam varius at. Cras ornare sit amet lacus vel elementum. Sed bibendum cursus pharetra. Ut cursus ultrices nisi, aliquet rhoncus metus commodo ac. Vestibulum elementum vestibulum nibh tempor ornare. Etiam mollis arcu vitae mi congue, quis congue massa ultrices. Sed volutpat, tortor posuere tristique ullamcorper, sapien est pretium justo, a ullamcorper velit ipsum vitae nibh. Donec bibendum eu risus id congue.

Maecenas varius feugiat orci vel laoreet. Aliquam erat volutpat. Quisque eu diam ex. Praesent elementum nibh a purus suscipit, lobortis tincidunt lacus vulputate. Sed tellus ex, elementum non enim vel, condimentum posuere lacus. Sed porttitor, diam sit amet ornare rhoncus, augue nulla eleifend mauris, et fermentum dolor nisi eu ipsum. Nam nec tempor massa. Curabitur mi nunc, ullamcorper vel euismod at, aliquam dapibus nisl. Praesent fermentum pellentesque libero eu blandit. Proin lacinia malesuada nisi. Pellentesque sagittis at sem ac pretium. Nulla sed viverra leo, vel mattis eros. Sed interdum augue vel dui egestas laoreet. Duis iaculis venenatis est, id lacinia tellus tempor vitae. Nulla bibendum bibendum quam. Mauris vestibulum eleifend mauris, et pretium dolor euismod id.

Quisque mi mauris, facilisis in mauris non, finibus tempor urna. Integer posuere venenatis urna, tincidunt pellentesque enim hendrerit sit amet. Sed pretium arcu sed condimentum pretium. Aenean accumsan malesuada ante, a ultricies lorem porttitor sit amet. Vestibulum eu tellus ultricies, hendrerit orci vitae, tincidunt nunc. Vestibulum elementum lectus a orci tristique dapibus quis id nisi. Curabitur augue arcu, pellentesque a mattis quis, suscipit feugiat mi. Pellentesque accumsan dolor vitae libero euismod, in feugiat lorem faucibus. Fusce a viverra metus. Mauris condimentum eget sem id dictum. Nunc at libero ante. Nulla mollis, mauris id suscipit tincidunt, tortor enim dignissim enim, non venenatis dolor ipsum in sapien. Fusce ligula ligula, placerat et dui sed, gravida iaculis urna. Nunc bibendum commodo felis et auctor. Maecenas ornare euismod nunc, suscipit dapibus orci faucibus a. Integer varius sapien nec lacus rhoncus faucibus.

Nulla non convallis mauris. Nullam iaculis eu metus vitae malesuada. Praesent ut viverra quam. Integer bibendum ante et purus pulvinar, vitae fringilla dui interdum. Nunc ut leo a lorem volutpat finibus. Morbi vel pulvinar lectus, non lobortis neque. Maecenas ultrices turpis in sodales placerat. Nam vitae orci non eros cursus posuere. Aenean eu suscipit metus, eu consectetur enim. Nullam vehicula tempus dolor quis fringilla. Vestibulum interdum quam velit, nec dapibus leo tempus at.

Pellentesque aliquet lobortis erat ac ullamcorper. Proin facilisis ligula tortor, sed sollicitudin libero porta a. Sed urna lorem, tempus vel diam nec, aliquam semper lectus. Phasellus vestibulum, dui ac dapibus consequat, arcu ipsum pharetra leo, quis accumsan arcu ligula vel orci. In lobortis magna nec dignissim vehicula. Aliquam sollicitudin auctor nisi eget lacinia. Morbi ut lobortis mi, sed dignissim nisl. Nunc tempor, nisi ut egestas varius, leo enim scelerisque dolor, in porttitor massa ante sit amet neque. Aliquam at porta mauris. Donec vestibulum interdum lacus, quis pulvinar eros commodo sit amet. Maecenas efficitur ante sed metus sollicitudin lobortis. Vivamus malesuada velit vel orci vehicula, eget malesuada lectus interdum. Duis vehicula ullamcorper est, condimentum ultrices magna mattis eget.

Duis scelerisque ornare ex, ut luctus urna tincidunt eu. Duis leo lacus, luctus nec erat eget, cursus facilisis quam. Donec at vestibulum mi. Nullam accumsan, metus vel convallis volutpat, lorem sapien laoreet felis, ut tempus neque ex sed augue. Maecenas laoreet mi quis tortor vehicula aliquam. Cras scelerisque ac purus vehicula placerat. Suspendisse ac lectus quis nibh scelerisque laoreet. Nullam id mauris viverra, ullamcorper mi ac, dignissim justo. Fusce sed felis fermentum leo eleifend laoreet. Mauris fermentum condimentum purus non congue.

Phasellus egestas lectus in ex pretium sodales. Duis in egestas eros. Aenean sed eros lobortis, vulputate metus quis, iaculis neque. Mauris finibus ac nunc et pulvinar. Quisque cursus sapien id ante consectetur, eu tincidunt lorem tincidunt. Suspendisse purus massa, luctus nec neque non, consequat aliquam leo. Nunc dolor sapien, convallis vitae congue fermentum, lacinia a lectus. Sed sit amet sem at urna dignissim laoreet sed facilisis dui. Praesent id lacinia turpis, non fringilla ex. Mauris pellentesque, leo eget dignissim mollis, ex massa laoreet metus, sed pharetra mi nulla vestibulum justo. Proin vel pulvinar libero. Nulla erat mi, condimentum sit amet massa at, feugiat sollicitudin dui. Nulla sit amet consectetur mi. Mauris eu faucibus nibh.

Nulla eget hendrerit quam, vel malesuada tortor. Aenean at dapibus orci. Etiam metus nibh, porttitor et tellus nec, venenatis blandit dui. Ut ac orci quis eros lacinia tincidunt. Praesent aliquet est in massa tempor, ac malesuada lorem gravida. Nulla tincidunt quam at ligula auctor, a tempus dolor varius. In suscipit vitae nibh eget aliquet.

Nulla ut magna tempor, scelerisque nibh vel, vestibulum velit. Phasellus ultricies nulla blandit dictum pellentesque. Pellentesque rhoncus ex neque, in suscipit dui suscipit sit amet. Phasellus ac posuere felis. Phasellus vitae erat et quam gravida feugiat id vitae lacus. Mauris cursus nibh sed quam fringilla, in dignissim velit vestibulum. Aliquam non mollis lectus. Maecenas congue euismod enim, ut maximus orci feugiat non. Nunc tortor metus, mollis vel turpis finibus, blandit rhoncus tellus. Duis id auctor elit. Proin euismod dapibus maximus. Phasellus non metus id ipsum scelerisque malesuada ac a dolor.
"""
}
