-- Query user1_id, user2_id for resources
select c.id,c.user1_id,c.user2_id,m.id,m.sender_id,m.folder_id,r.id,
CONCAT("UPDATE Resource SET authorizedUserIds='",c.user1_id,",",c.user2_id,"' WHERE id=",r.id,";") 
from conversation c, message m, resource r where m.folder_id is not null and m.conversation_id=c.id and m.folder_id=r.folder_id;

+-----+----------+----------+------+-----------+-----------+-----+----------------------------------------------------------------------------------------------------+
| id  | user1_id | user2_id | id   | sender_id | folder_id | id  | CONCAT("UPDATE Resource SET authorizedUserIds='",c.user1_id,",",c.user2_id,"' WHERE id=",r.id,";") |
+-----+----------+----------+------+-----------+-----------+-----+----------------------------------------------------------------------------------------------------+
|  17 |        2 |        4 |  125 |         2 |        73 | 158 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=158;                                          |
|  17 |        2 |        4 |  126 |         4 |        74 | 159 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=159;                                          |
|   9 |        2 |        4 |  206 |         4 |        91 | 187 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=187;                                          |
|  16 |        9 |        2 |  272 |         2 |       110 | 231 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=231;                                          |
|  16 |        9 |        2 |  273 |         2 |       112 | 233 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=233;                                          |
|  16 |        9 |        2 |  274 |         2 |       113 | 234 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=234;                                          |
|  82 |        9 |        2 |  306 |         2 |       114 | 247 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=247;                                          |
|  28 |        2 |        4 |  344 |         2 |       120 | 253 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=253;                                          |
|  38 |        2 |        4 |  354 |         2 |       121 | 254 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=254;                                          |
|  92 |       62 |        2 |  364 |         2 |       132 | 266 | UPDATE Resource SET authorizedUserIds='62,2' WHERE id=266;                                         |
|  88 |        2 |        4 |  365 |         2 |       133 | 267 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=267;                                          |
|  92 |       62 |        2 |  366 |         2 |       134 | 268 | UPDATE Resource SET authorizedUserIds='62,2' WHERE id=268;                                         |
|  82 |        9 |        2 |  454 |         2 |       137 | 272 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=272;                                          |
|  82 |        9 |        2 |  455 |         2 |       138 | 273 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=273;                                          |
|  96 |       69 |       23 |  481 |        69 |       139 | 274 | UPDATE Resource SET authorizedUserIds='69,23' WHERE id=274;                                        |
|  91 |       61 |       23 |  526 |        61 |       142 | 279 | UPDATE Resource SET authorizedUserIds='61,23' WHERE id=279;                                        |
|  99 |       75 |        7 |  530 |         7 |       143 | 280 | UPDATE Resource SET authorizedUserIds='75,7' WHERE id=280;                                         |
|  97 |       74 |       23 |  535 |        74 |       145 | 284 | UPDATE Resource SET authorizedUserIds='74,23' WHERE id=284;                                        |
|  82 |        9 |        2 |  540 |         2 |       146 | 285 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=285;                                          |
|  93 |       64 |       23 |  642 |        64 |       149 | 289 | UPDATE Resource SET authorizedUserIds='64,23' WHERE id=289;                                        |
| 121 |      100 |       23 |  763 |       100 |       152 | 292 | UPDATE Resource SET authorizedUserIds='100,23' WHERE id=292;                                       |
| 145 |      121 |       23 |  968 |       121 |       153 | 293 | UPDATE Resource SET authorizedUserIds='121,23' WHERE id=293;                                       |
| 120 |       99 |       23 | 1000 |        99 |       154 | 294 | UPDATE Resource SET authorizedUserIds='99,23' WHERE id=294;                                        |
| 144 |      120 |       23 | 1012 |       120 |       155 | 295 | UPDATE Resource SET authorizedUserIds='120,23' WHERE id=295;                                       |
| 144 |      120 |       23 | 1017 |       120 |       156 | 296 | UPDATE Resource SET authorizedUserIds='120,23' WHERE id=296;                                       |
| 137 |      113 |       23 | 1070 |       113 |       158 | 300 | UPDATE Resource SET authorizedUserIds='113,23' WHERE id=300;                                       |
| 137 |      113 |       23 | 1071 |       113 |       159 | 301 | UPDATE Resource SET authorizedUserIds='113,23' WHERE id=301;                                       |
| 132 |      109 |       23 | 1086 |       109 |       160 | 302 | UPDATE Resource SET authorizedUserIds='109,23' WHERE id=302;                                       |
| 158 |        7 |       98 | 1140 |        98 |       161 | 303 | UPDATE Resource SET authorizedUserIds='7,98' WHERE id=303;                                         |
| 122 |       95 |       23 | 1254 |        95 |       168 | 316 | UPDATE Resource SET authorizedUserIds='95,23' WHERE id=316;                                        |
| 105 |       80 |       23 | 1273 |        80 |       169 | 317 | UPDATE Resource SET authorizedUserIds='80,23' WHERE id=317;                                        |
| 105 |       80 |       23 | 1283 |        80 |       170 | 318 | UPDATE Resource SET authorizedUserIds='80,23' WHERE id=318;                                        |
| 140 |      118 |       23 | 1294 |       118 |       171 | 319 | UPDATE Resource SET authorizedUserIds='118,23' WHERE id=319;                                       |
|  90 |       60 |       23 | 1308 |        60 |       172 | 320 | UPDATE Resource SET authorizedUserIds='60,23' WHERE id=320;                                        |
| 108 |       84 |       23 | 1346 |        84 |       173 | 321 | UPDATE Resource SET authorizedUserIds='84,23' WHERE id=321;                                        |
| 106 |       79 |       23 | 1347 |        79 |       174 | 322 | UPDATE Resource SET authorizedUserIds='79,23' WHERE id=322;                                        |
| 129 |      105 |       23 | 1356 |       105 |       175 | 323 | UPDATE Resource SET authorizedUserIds='105,23' WHERE id=323;                                       |
|  82 |        9 |        2 | 1404 |         2 |       177 | 325 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=325;                                          |
|  88 |        2 |        4 | 1405 |         2 |       178 | 326 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=326;                                          |
| 116 |       91 |       23 | 1423 |        91 |       179 | 327 | UPDATE Resource SET authorizedUserIds='91,23' WHERE id=327;                                        |
| 127 |       98 |       23 | 1504 |        98 |       181 | 329 | UPDATE Resource SET authorizedUserIds='98,23' WHERE id=329;                                        |
| 176 |      150 |       23 | 1509 |       150 |       182 | 330 | UPDATE Resource SET authorizedUserIds='150,23' WHERE id=330;                                       |
| 128 |      104 |       23 | 1510 |       104 |       183 | 331 | UPDATE Resource SET authorizedUserIds='104,23' WHERE id=331;                                       |
| 155 |      129 |       23 | 1531 |       129 |       184 | 332 | UPDATE Resource SET authorizedUserIds='129,23' WHERE id=332;                                       |
| 174 |      145 |       23 | 1540 |       145 |       185 | 333 | UPDATE Resource SET authorizedUserIds='145,23' WHERE id=333;                                       |
| 157 |       88 |       23 | 1542 |        88 |       186 | 334 | UPDATE Resource SET authorizedUserIds='88,23' WHERE id=334;                                        |
| 121 |      100 |       23 | 1548 |       100 |       187 | 335 | UPDATE Resource SET authorizedUserIds='100,23' WHERE id=335;                                       |
| 121 |      100 |       23 | 1549 |       100 |       188 | 336 | UPDATE Resource SET authorizedUserIds='100,23' WHERE id=336;                                       |
| 131 |      108 |       23 | 1555 |       108 |       189 | 337 | UPDATE Resource SET authorizedUserIds='108,23' WHERE id=337;                                       |
| 139 |      115 |       23 | 1556 |       115 |       190 | 338 | UPDATE Resource SET authorizedUserIds='115,23' WHERE id=338;                                       |
| 111 |       86 |       23 | 1561 |        86 |       191 | 339 | UPDATE Resource SET authorizedUserIds='86,23' WHERE id=339;                                        |
| 133 |       96 |       23 | 1562 |        96 |       192 | 340 | UPDATE Resource SET authorizedUserIds='96,23' WHERE id=340;                                        |
| 114 |       90 |       23 | 1574 |        90 |       193 | 341 | UPDATE Resource SET authorizedUserIds='90,23' WHERE id=341;                                        |
| 125 |       97 |       23 | 1575 |        97 |       194 | 342 | UPDATE Resource SET authorizedUserIds='97,23' WHERE id=342;                                        |
| 113 |       87 |       23 | 1577 |        87 |       195 | 343 | UPDATE Resource SET authorizedUserIds='87,23' WHERE id=343;                                        |
| 104 |       81 |       23 | 1581 |        81 |       196 | 344 | UPDATE Resource SET authorizedUserIds='81,23' WHERE id=344;                                        |
|  82 |        9 |        2 | 1591 |         9 |       197 | 345 | UPDATE Resource SET authorizedUserIds='9,2' WHERE id=345;                                          |
| 150 |      124 |       23 | 1606 |       124 |       198 | 346 | UPDATE Resource SET authorizedUserIds='124,23' WHERE id=346;                                       |
| 100 |       76 |       23 | 1616 |        23 |       200 | 348 | UPDATE Resource SET authorizedUserIds='76,23' WHERE id=348;                                        |
| 152 |      124 |        4 | 1620 |         4 |       201 | 349 | UPDATE Resource SET authorizedUserIds='124,4' WHERE id=349;                                        |
| 152 |      124 |        4 | 1621 |         4 |       202 | 350 | UPDATE Resource SET authorizedUserIds='124,4' WHERE id=350;                                        |
| 135 |      110 |       23 | 1665 |       110 |       206 | 355 | UPDATE Resource SET authorizedUserIds='110,23' WHERE id=355;                                       |
| 161 |      134 |       23 | 1671 |       134 |       207 | 356 | UPDATE Resource SET authorizedUserIds='134,23' WHERE id=356;                                       |
| 164 |      139 |       23 | 1673 |       139 |       208 | 357 | UPDATE Resource SET authorizedUserIds='139,23' WHERE id=357;                                       |
| 138 |      114 |       23 | 1680 |       114 |       209 | 358 | UPDATE Resource SET authorizedUserIds='114,23' WHERE id=358;                                       |
| 175 |        9 |       23 | 1705 |         9 |       210 | 359 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=359;                                         |
| 166 |      142 |       23 | 1728 |       142 |       212 | 362 | UPDATE Resource SET authorizedUserIds='142,23' WHERE id=362;                                       |
| 165 |      141 |       23 | 1730 |       141 |       213 | 363 | UPDATE Resource SET authorizedUserIds='141,23' WHERE id=363;                                       |
| 165 |      141 |       23 | 1733 |       141 |       214 | 364 | UPDATE Resource SET authorizedUserIds='141,23' WHERE id=364;                                       |
| 118 |       94 |       23 | 1751 |        94 |       215 | 365 | UPDATE Resource SET authorizedUserIds='94,23' WHERE id=365;                                        |
| 177 |      153 |       23 | 1782 |       153 |       216 | 366 | UPDATE Resource SET authorizedUserIds='153,23' WHERE id=366;                                       |
|  98 |       75 |       23 | 1788 |        75 |       217 | 367 | UPDATE Resource SET authorizedUserIds='75,23' WHERE id=367;                                        |
| 159 |      131 |       23 | 1809 |       131 |       218 | 368 | UPDATE Resource SET authorizedUserIds='131,23' WHERE id=368;                                       |
| 175 |        9 |       23 | 1832 |        23 |       219 | 369 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=369;                                         |
| 175 |        9 |       23 | 1833 |        23 |       220 | 370 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=370;                                         |
| 153 |      127 |       23 | 1840 |       127 |       221 | 371 | UPDATE Resource SET authorizedUserIds='127,23' WHERE id=371;                                       |
| 153 |      127 |       23 | 1842 |       127 |       222 | 372 | UPDATE Resource SET authorizedUserIds='127,23' WHERE id=372;                                       |
| 172 |      148 |       23 | 1855 |       148 |       223 | 373 | UPDATE Resource SET authorizedUserIds='148,23' WHERE id=373;                                       |
| 172 |      148 |       23 | 1865 |       148 |       224 | 374 | UPDATE Resource SET authorizedUserIds='148,23' WHERE id=374;                                       |
| 124 |      102 |       23 | 1885 |       102 |       229 | 379 | UPDATE Resource SET authorizedUserIds='102,23' WHERE id=379;                                       |
| 184 |        4 |       96 | 1898 |        96 |       230 | 380 | UPDATE Resource SET authorizedUserIds='4,96' WHERE id=380;                                         |
| 184 |        4 |       96 | 1899 |        96 |       231 | 381 | UPDATE Resource SET authorizedUserIds='4,96' WHERE id=381;                                         |
| 126 |      103 |       23 | 1918 |       103 |       232 | 382 | UPDATE Resource SET authorizedUserIds='103,23' WHERE id=382;                                       |
| 126 |      103 |       23 | 1919 |       103 |       233 | 383 | UPDATE Resource SET authorizedUserIds='103,23' WHERE id=383;                                       |
| 152 |      124 |        4 | 1936 |       124 |       234 | 384 | UPDATE Resource SET authorizedUserIds='124,4' WHERE id=384;                                        |
| 175 |        9 |       23 | 1968 |        23 |       237 | 388 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=388;                                         |
| 175 |        9 |       23 | 1969 |        23 |       238 | 389 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=389;                                         |
| 175 |        9 |       23 | 1973 |        23 |       240 | 391 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=391;                                         |
| 152 |      124 |        4 | 2019 |         4 |       242 | 395 | UPDATE Resource SET authorizedUserIds='124,4' WHERE id=395;                                        |
| 152 |      124 |        4 | 2020 |         4 |       243 | 396 | UPDATE Resource SET authorizedUserIds='124,4' WHERE id=396;                                        |
|  88 |        2 |        4 | 2023 |         4 |       244 | 397 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=397;                                          |
|  88 |        2 |        4 | 2024 |         4 |       245 | 398 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=398;                                          |
|  88 |        2 |        4 | 2025 |         4 |       246 | 399 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=399;                                          |
|  88 |        2 |        4 | 2026 |         4 |       247 | 400 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=400;                                          |
|  88 |        2 |        4 | 2027 |         4 |       248 | 401 | UPDATE Resource SET authorizedUserIds='2,4' WHERE id=401;                                          |
| 170 |        7 |      135 | 2034 |         7 |       249 | 402 | UPDATE Resource SET authorizedUserIds='7,135' WHERE id=402;                                        |
| 171 |      144 |       23 | 2052 |       144 |       250 | 403 | UPDATE Resource SET authorizedUserIds='144,23' WHERE id=403;                                       |
| 183 |        5 |      108 | 2063 |         5 |       251 | 404 | UPDATE Resource SET authorizedUserIds='5,108' WHERE id=404;                                        |
| 183 |        5 |      108 | 2065 |       108 |       252 | 405 | UPDATE Resource SET authorizedUserIds='5,108' WHERE id=405;                                        |
| 175 |        9 |       23 | 2079 |        23 |       253 | 406 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=406;                                         |
| 152 |      124 |        4 | 2135 |         4 |       254 | 407 | UPDATE Resource SET authorizedUserIds='124,4' WHERE id=407;                                        |
| 175 |        9 |       23 | 2222 |        23 |       262 | 417 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=417;                                         |
| 175 |        9 |       23 | 2253 |        23 |       267 | 423 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=423;                                         |
| 199 |      196 |        7 | 2270 |         7 |       269 | 425 | UPDATE Resource SET authorizedUserIds='196,7' WHERE id=425;                                        |
| 199 |      196 |        7 | 2273 |         7 |       270 | 426 | UPDATE Resource SET authorizedUserIds='196,7' WHERE id=426;                                        |
| 199 |      196 |        7 | 2278 |         7 |       271 | 427 | UPDATE Resource SET authorizedUserIds='196,7' WHERE id=427;                                        |
| 199 |      196 |        7 | 2280 |         7 |       272 | 428 | UPDATE Resource SET authorizedUserIds='196,7' WHERE id=428;                                        |
| 175 |        9 |       23 | 2295 |        23 |       275 | 432 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=432;                                         |
| 201 |        6 |      108 | 2315 |         6 |       283 | 444 | UPDATE Resource SET authorizedUserIds='6,108' WHERE id=444;                                        |
| 201 |        6 |      108 | 2322 |       108 |       319 | 533 | UPDATE Resource SET authorizedUserIds='6,108' WHERE id=533;                                        |
| 175 |        9 |       23 | 2326 |        23 |       326 | 542 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=542;                                         |
| 175 |        9 |       23 | 2328 |         9 |       327 | 543 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=543;                                         |
| 175 |        9 |       23 | 2331 |        23 |       329 | 546 | UPDATE Resource SET authorizedUserIds='9,23' WHERE id=546;                                         |
| 219 |       23 |      225 | 2367 |        23 |       335 | 556 | UPDATE Resource SET authorizedUserIds='23,225' WHERE id=556;                                       |
+-----+----------+----------+------+-----------+-----------+-----+----------------------------------------------------------------------------------------------------+
114 rows in set (0.03 sec)






-- Actual SQLs
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=158; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=159; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=187; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=231; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=233; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=234; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=247; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=253; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=254; 
 UPDATE Resource SET authorizedUserIds='62,2' WHERE id=266;  
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=267; 
 UPDATE Resource SET authorizedUserIds='62,2' WHERE id=268;  
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=272; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=273; 
 UPDATE Resource SET authorizedUserIds='69,23' WHERE id=274; 
 UPDATE Resource SET authorizedUserIds='61,23' WHERE id=279; 
 UPDATE Resource SET authorizedUserIds='75,7' WHERE id=280;  
 UPDATE Resource SET authorizedUserIds='74,23' WHERE id=284; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=285; 
 UPDATE Resource SET authorizedUserIds='64,23' WHERE id=289; 
 UPDATE Resource SET authorizedUserIds='100,23' WHERE id=292;  
 UPDATE Resource SET authorizedUserIds='121,23' WHERE id=293;  
 UPDATE Resource SET authorizedUserIds='99,23' WHERE id=294; 
 UPDATE Resource SET authorizedUserIds='120,23' WHERE id=295;  
 UPDATE Resource SET authorizedUserIds='120,23' WHERE id=296;  
 UPDATE Resource SET authorizedUserIds='113,23' WHERE id=300;  
 UPDATE Resource SET authorizedUserIds='113,23' WHERE id=301;  
 UPDATE Resource SET authorizedUserIds='109,23' WHERE id=302;  
 UPDATE Resource SET authorizedUserIds='7,98' WHERE id=303;  
 UPDATE Resource SET authorizedUserIds='95,23' WHERE id=316; 
 UPDATE Resource SET authorizedUserIds='80,23' WHERE id=317; 
 UPDATE Resource SET authorizedUserIds='80,23' WHERE id=318; 
 UPDATE Resource SET authorizedUserIds='118,23' WHERE id=319;  
 UPDATE Resource SET authorizedUserIds='60,23' WHERE id=320; 
 UPDATE Resource SET authorizedUserIds='84,23' WHERE id=321; 
 UPDATE Resource SET authorizedUserIds='79,23' WHERE id=322; 
 UPDATE Resource SET authorizedUserIds='105,23' WHERE id=323;  
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=325; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=326; 
 UPDATE Resource SET authorizedUserIds='91,23' WHERE id=327; 
 UPDATE Resource SET authorizedUserIds='98,23' WHERE id=329; 
 UPDATE Resource SET authorizedUserIds='150,23' WHERE id=330;  
 UPDATE Resource SET authorizedUserIds='104,23' WHERE id=331;  
 UPDATE Resource SET authorizedUserIds='129,23' WHERE id=332;  
 UPDATE Resource SET authorizedUserIds='145,23' WHERE id=333;  
 UPDATE Resource SET authorizedUserIds='88,23' WHERE id=334; 
 UPDATE Resource SET authorizedUserIds='100,23' WHERE id=335;  
 UPDATE Resource SET authorizedUserIds='100,23' WHERE id=336;  
 UPDATE Resource SET authorizedUserIds='108,23' WHERE id=337;  
 UPDATE Resource SET authorizedUserIds='115,23' WHERE id=338;  
 UPDATE Resource SET authorizedUserIds='86,23' WHERE id=339; 
 UPDATE Resource SET authorizedUserIds='96,23' WHERE id=340; 
 UPDATE Resource SET authorizedUserIds='90,23' WHERE id=341; 
 UPDATE Resource SET authorizedUserIds='97,23' WHERE id=342; 
 UPDATE Resource SET authorizedUserIds='87,23' WHERE id=343; 
 UPDATE Resource SET authorizedUserIds='81,23' WHERE id=344; 
 UPDATE Resource SET authorizedUserIds='9,2' WHERE id=345; 
 UPDATE Resource SET authorizedUserIds='124,23' WHERE id=346;  
 UPDATE Resource SET authorizedUserIds='76,23' WHERE id=348; 
 UPDATE Resource SET authorizedUserIds='124,4' WHERE id=349; 
 UPDATE Resource SET authorizedUserIds='124,4' WHERE id=350; 
 UPDATE Resource SET authorizedUserIds='110,23' WHERE id=355;  
 UPDATE Resource SET authorizedUserIds='134,23' WHERE id=356;  
 UPDATE Resource SET authorizedUserIds='139,23' WHERE id=357;  
 UPDATE Resource SET authorizedUserIds='114,23' WHERE id=358;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=359;  
 UPDATE Resource SET authorizedUserIds='142,23' WHERE id=362;  
 UPDATE Resource SET authorizedUserIds='141,23' WHERE id=363;  
 UPDATE Resource SET authorizedUserIds='141,23' WHERE id=364;  
 UPDATE Resource SET authorizedUserIds='94,23' WHERE id=365; 
 UPDATE Resource SET authorizedUserIds='153,23' WHERE id=366;  
 UPDATE Resource SET authorizedUserIds='75,23' WHERE id=367; 
 UPDATE Resource SET authorizedUserIds='131,23' WHERE id=368;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=369;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=370;  
 UPDATE Resource SET authorizedUserIds='127,23' WHERE id=371;  
 UPDATE Resource SET authorizedUserIds='127,23' WHERE id=372;  
 UPDATE Resource SET authorizedUserIds='148,23' WHERE id=373;  
 UPDATE Resource SET authorizedUserIds='148,23' WHERE id=374;  
 UPDATE Resource SET authorizedUserIds='102,23' WHERE id=379;  
 UPDATE Resource SET authorizedUserIds='4,96' WHERE id=380;  
 UPDATE Resource SET authorizedUserIds='4,96' WHERE id=381;  
 UPDATE Resource SET authorizedUserIds='103,23' WHERE id=382;  
 UPDATE Resource SET authorizedUserIds='103,23' WHERE id=383;  
 UPDATE Resource SET authorizedUserIds='124,4' WHERE id=384; 
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=388;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=389;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=391;  
 UPDATE Resource SET authorizedUserIds='124,4' WHERE id=395; 
 UPDATE Resource SET authorizedUserIds='124,4' WHERE id=396; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=397; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=398; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=399; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=400; 
 UPDATE Resource SET authorizedUserIds='2,4' WHERE id=401; 
 UPDATE Resource SET authorizedUserIds='7,135' WHERE id=402; 
 UPDATE Resource SET authorizedUserIds='144,23' WHERE id=403;  
 UPDATE Resource SET authorizedUserIds='5,108' WHERE id=404; 
 UPDATE Resource SET authorizedUserIds='5,108' WHERE id=405; 
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=406;  
 UPDATE Resource SET authorizedUserIds='124,4' WHERE id=407; 
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=417;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=423;  
 UPDATE Resource SET authorizedUserIds='196,7' WHERE id=425; 
 UPDATE Resource SET authorizedUserIds='196,7' WHERE id=426; 
 UPDATE Resource SET authorizedUserIds='196,7' WHERE id=427; 
 UPDATE Resource SET authorizedUserIds='196,7' WHERE id=428; 
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=432;  
 UPDATE Resource SET authorizedUserIds='6,108' WHERE id=444; 
 UPDATE Resource SET authorizedUserIds='6,108' WHERE id=533; 
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=542;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=543;  
 UPDATE Resource SET authorizedUserIds='9,23' WHERE id=546;  
 UPDATE Resource SET authorizedUserIds='23,225' WHERE id=556;
