package com.example.domain.mitzvot

import java.time.LocalDate

enum class MitzvahAssignmentType { POSITIVE, NEGATIVE, SPECIAL }

data class MitzvahAssignmentItem(
    val type: MitzvahAssignmentType,
    val number: Int? = null,
    val title: String? = null
)

data class DailyMitzvahAssignment(
    val cycleNumber: Int,
    val lessonNumber: Int,
    val studyDate: LocalDate,
    val items: List<MitzvahAssignmentItem>
)

/** Official cycle 46 schedule: 16 Shevat 5786 through 28 Tevet 5787. */
object SeferHamitzvotSchedule {
    const val CYCLE_NUMBER = 46
    const val SOURCE_URL = "https://www.chabad.org/media/pdf/526/eWnk5263924.pdf"
    private val firstDay = LocalDate.of(2026, 2, 3)

    private val assignments: List<DailyMitzvahAssignment> by lazy {
        rawSchedule.lineSequence()
            .filter { it.isNotBlank() }
            .map(::parseLine)
            .toList()
            .also { parsed ->
                require(parsed.size == 339) { "Cycle 46 must contain 339 lessons" }
                require(parsed.map { it.lessonNumber } == (1..339).toList()) {
                    "Cycle 46 lessons must be consecutive"
                }
            }
    }

    fun assignmentFor(date: LocalDate): DailyMitzvahAssignment? =
        assignments.getOrNull(date.toEpochDay().minus(firstDay.toEpochDay()).toInt())

    fun allAssignments(): List<DailyMitzvahAssignment> = assignments

    private fun parseLine(line: String): DailyMitzvahAssignment {
        val separator = line.indexOf('|')
        require(separator > 0) { "Invalid schedule line: $line" }
        val lesson = line.substring(0, separator).toInt()
        val items = line.substring(separator + 1)
            .split(',')
            .filter { it.isNotBlank() }
            .map { token ->
                when {
                    token.startsWith("P") -> MitzvahAssignmentItem(
                        MitzvahAssignmentType.POSITIVE,
                        number = token.drop(1).toInt()
                    )
                    token.startsWith("N") -> MitzvahAssignmentItem(
                        MitzvahAssignmentType.NEGATIVE,
                        number = token.drop(1).toInt()
                    )
                    token.startsWith("S:") -> MitzvahAssignmentItem(
                        MitzvahAssignmentType.SPECIAL,
                        title = token.removePrefix("S:")
                    )
                    else -> error("Unknown assignment token: $token")
                }
            }
        return DailyMitzvahAssignment(
            cycleNumber = CYCLE_NUMBER,
            lessonNumber = lesson,
            studyDate = firstDay.plusDays((lesson - 1).toLong()),
            items = items
        )
    }

    private val rawSchedule = """
1|S:הקדמת הרמב״ם לספר המצוות
2|S:י״ד השורשים
3|S:י״ד השורשים
4|S:י״ד השורשים
5|P1,N1,P2
6|P3,P4,P9
7|N63,N65,P172
8|N64,P8,P6,P206
9|P207,N302,P205,N303
10|N256,N301,N304,N305
11|P11
12|P209
13|N10,N47,N60,N6,N5,N2,N3,N4,N15,P186,N23,N24
14|N16,N17,N18,N19,N20,N21,N26,N28,N27,N29,N14,N8,N9,N7
15|N11,N12,N13,P185,N25,N22,N48,N50,N51,N30,N33,N31,N32
16|N35,N38,N36,N37,N34,N43,N44,N40,N39,N41,N45,N171
17|P73
18|P73
19|P73
20|P73,P10
21|P10,P5
22|P5
23|P5
24|P5
25|P5
26|P26,P12
27|P13
28|P15,P18
29|P17
30|P14
31|P19
32|P19
33|P19
34|P19,P215
35|P215,S:סוף ספר אהבה – סדר התפילות
36|S:נוסח ברכות התפילה וסידורן
37|P154,S:נוסח ברכת המזון
38|P154
39|N320
40|N320
41|N322
42|N322
43|N321
44|N321
45|P155
46|P155
47|P155
48|N320
49|N321
50|P165,N329,P164,N196
51|P159,N323,P160,N324
52|P162,N325,P163,N326
53|P166,N327,P167,N328,N199
54|P156,N197,N198
55|N200,N201,P158
56|P157,P170,S:סוף הלכות חמץ ומצה – נוסח ההגדה
57|P168
58|P169
59|P171
60|P171,P153
61|P153
62|P153
63|P153
64|P153
65|P153
66|P153,P59
67|P59
68|P59,S:הלכות מגילה וחנוכה פרקים א–ב
69|P213,S:הלכות מגילה וחנוכה פרקים ג–ד
70|P213
71|N355
72|N355
73|N355
74|N262
75|N262
76|P212
77|P212
78|P222
79|P222
80|N356
81|N356
82|N356,P216
83|P217
84|N357
85|P220,P218,N358,P219,N359
86|P223,N104
87|N105,N330,N331,N332,N333,N334
88|N336,N335,N337,N338,N339
89|N340,N341,N342,N343,N344,N345
90|N348,N349,N350,N351
91|N352,N347,N346
92|N52,N53,N55,N54,N354,N360,N361
93|N161,N162,P38,N160,N158,N159
94|N353,P149
95|P150,P151,P152,N172,N174
96|N173,N175,N176,N177,N178
97|N179,N180,N188
98|N181,N182,N184,N185,N183
99|N187,N186,N189,N190,N191,N192
100|N193,N153,N194,P146
101|N101
102|P147
103|N306
104|P148
105|N61
106|N62
107|N248,N249
108|P7
109|P94
110|P94
111|N157
112|P95
113|P95,P92,N209
114|N202,N203,N204
115|N205,N206,N208,N207
116|P93,P114
117|P115,P116,P117
118|P145,N110
119|N111,N215
120|N216
121|N217,N218
122|N42,P120,N210
123|P121,N211,P123,N212
124|P124,N213,P122,N214
125|P130,P195,N232
126|P126,P129
127|N154
128|N133,N134
129|N135,N136
130|N137
131|P127
132|P127
133|P127
134|P127
135|P127,P128,N152
136|N150,N151
137|N141,N142,N143
138|P119
139|P131,P125
140|N149,P132
141|P133,P143,P144
142|P80,P81
143|P82,P135,N220,N221,N222
144|N223,P134,P141,N230,N231
145|P140,P136,P137,N224,N225,N226
146|P138,N227,P139
147|N169,N170,P183,N228,P20
148|N79,N80
149|P21
150|P22,N67,P35,N83,N84
151|N85,N82,P34,N86
152|P23,N72,P32,P36
153|P33,N88,N87,N73,N163,N164
154|N68,N165,P31,N77,N78
155|N75,N76,P24,N69,N70,N71
156|N74,P61,N91,N92,N93
157|N94,N95,N96,N97,P86
158|P60,N100,N98,P62,N99
159|P63,N146,P64
160|N139,N112,P65
161|P89,N145,N148
162|P66,N147,P67,N102,N103,N138
163|N124,P88,P83,N155
164|P84,P85,N90
165|N89,P39,P29,N81,P30
166|P28,P25,P40,P41,P27,P42
167|P43,P44,P45,P46,P47,P48,P50,P51
168|P161,N140
169|N132
170|N120
171|N131
172|N130,N129
173|P91
174|P90
175|P49
176|P49,P118
177|N113
178|N114
179|N114,P55,N115,N116
180|P57,P56,P58
181|N125,N123,N128,N126,N127,N121,N122
182|N117,N119,N118,P53,P52
183|P54,N156,N229,P16,P79
184|N144,N108
185|P78
186|N109,P69
187|P70
188|P70
189|P71
190|P72
191|P68,P75,P76
192|P74,P77
193|N106,P87
194|N107,P107
195|P107
196|P107
197|P107
198|P107
199|P107
200|P107
201|P107
202|P107,P113
203|P113
204|P113
205|P108
206|P108
207|P108,P101
208|N308,N307
209|P112
210|P110
211|P111
212|P102,P103
213|P99
214|P100
215|P106
216|P104
217|P104,P96
218|P96
219|P97
220|P97
221|P105
222|P105
223|P105
224|P98
225|P98
226|P98
227|P98
228|P98
229|P98
230|P107
231|P108
232|P101
233|P99
234|P100
235|P106
236|P104
237|P96
238|P109
239|P109
240|P109
241|P109
242|P109,P237
243|P240
244|P238
245|P241
246|P241
247|N244,P239
248|P208,N271,N272
249|N246,N243
250|N245
251|N247
252|N265
253|N266
254|P194
255|N269,P204
256|P236
257|P236
258|P236,N289,N296
259|P225,N295,N292
260|P247,N293,N297,P182
261|P181,N309,N298,P184
262|N299,P202,P203,N270
263|P245
264|P245
265|N250
266|N250
267|N251
268|N251
269|N253
270|N253
271|N252
272|N252
273|P245
274|P245
275|P245
276|P245
277|P236
278|P236
279|P236
280|P236
281|P245
282|P245
283|P245
284|P245
285|P232,N258,N259,N257,N260
286|P196,N233,P234,P233,N261
287|P235,N254,N255
288|P243
289|P200,N238
290|P201
291|N267,N268
292|N219,P244
293|P244
294|P242
295|P197,N234
296|P142
297|N239
298|P199,N240
299|N241,N242
300|N235
301|N236
302|N237
303|P198
304|P246
305|P246
306|P246
307|P246
308|P246
309|P246,P248
310|P248
311|P248
312|P248
313|P176,N284,P175
314|N282,N283,P229,P228
315|P226,P227,P230,P231,N66
316|N310,P224,N300
317|N294,N290
318|N279,N277,N275,N278,N273
319|N280,P177,N276,N274
320|N315,N281,N316
321|N317,P178
322|P179
323|N291
324|N288
325|N286
326|N287
327|N285
328|P180
329|P174,N312,N313,N314
330|N318,N319,P210,P211
331|N195,P37
332|N168
333|N167
334|N166
335|N166
336|P173,N362,N364,N363,N365
337|P187,N49,P188,P189,N59
338|N46,P190,N56,N57,P192,P193
339|P191,P214,N311,N58,P221,N263,N264
    """.trimIndent()
}
