package org.platzhaltr

import java.time.{DayOfWeek, Month}
import org.parboiled2._

trait ParseResult

class DateParser(val input: ParserInput) extends Parser {

  def InputLine = rule { Expression ~ EOI }

  def Expression: Rule1[ParseResult] = rule {
    DateTimes                                  ~> ((d: DateEvent,t: TimeEvent) => DateTimeEvent(d,t)) |
    Times                                      ~> ((t: TimeEvent) => t)|
    Dates                                      ~> ((d: DateEvent) => d)  |
    TimeDurations                              ~> ((d: TimeDuration) =>  d) |
    DateDurations                              ~> ((d: DateDuration) => d)
  }

  def DateTimes = rule {
    AbsoluteDateTimes |
    RelativeDateTimes
  }

  def RelativeDateTimes = rule {
    RelativeDates ~ Space ~ AbsoluteTimes
  }

  def AbsoluteDateTimes = rule {
    AbsoluteDates ~ Space ~ !(YearDigits) ~ AbsoluteTimes
  }

  def Times = rule {
    AbsoluteTimes | RelativeTimes
  }

  def Dates = rule {
    AbsoluteDates | RelativeDates
  }

  def AbsoluteTimes = rule {
    FormalTimes | FuzzyTimes
  }

  def FormalTimes = rule {
    TwelveHourTime                             ~> ((t) => AtTime(t)) |
    IsoTime                                    ~> ((t) => AtTime(t)) |
    At ~ Space ~ TwelveHourTime                ~> ((t) => AtTime(t)) |
    At ~ Space ~ IsoTime                       ~> ((t) => AtTime(t))
  }

  def FuzzyTimes = rule {
    Noon                                       ~> ((t) => AtTime(t)) |
    Afternoon                                  ~> ((t) => AtTime(t)) |
    Evening                                    ~> ((t) => AtTime(t)) |
    Midnight                                   ~> ((t) => AtTime(t))
  }

  def RelativeTimes = rule {
    In ~ Space ~ Number ~ Space ~ Seconds      ~> ((s) => InSeconds(s)) |
    In ~ Space ~ Number ~ Space ~ Minutes      ~> ((m) => InMinutes(m)) |
    In ~ Space ~ Number ~ Space ~ Hours        ~> ((h) => InHours(h)) |
    Number ~ Space ~ Seconds ~ Space ~ Ago     ~> ((s) => InSeconds(-s)) |
    Number ~ Space ~ Minutes ~ Space ~ Ago     ~> ((m) => InMinutes(-m)) |
    Number ~ Space ~ Hours ~ Space ~ Ago       ~> ((h) => InHours(-h)) |
    Number ~ Space ~ Seconds ~ Space ~ FromNow ~> ((s) => InSeconds(s)) |
    Number ~ Space ~ Minutes ~ Space ~ FromNow ~> ((m) => InMinutes(m)) |
    Number ~ Space ~ Hours ~ Space ~ FromNow   ~> ((h) => InHours(h))
  }

  def AbsoluteDates = rule {
    optional(On ~ Space) ~ (FormalDate | IsoDate | LittleEndianDate) ~> ((d) => OnDate(d))
  }

  def RelativeDates = rule {
    RelativeDays |
    RelativeDatesFuture |
    RelativeDatesPast |
    CardinalWeekdayInMonth
  }

  def CardinalWeekdayInMonth = rule {
    Cardinal ~ Space ~ SpecificWeekday ~ Space ~ (In | Of) ~ Space ~ SpecificMonth ~> ((c,w,m) => WeekdayInMonth(c, w, m))
  }

  def RelativeDays = rule {
    Today                                      ~> (()  => InDays(0)) |
    Tomorrow                                   ~> (()  => InDays(1)) |
    Yesterday                                  ~> (()  => InDays(-1))
  }

  def RelativeDatesFuture = rule {
    SpecificWeekday                            ~> ((w) => NextWeekdayByName(w)) |
    Next ~ Space ~ Months                      ~> (()  => InMonths(1)) |
    Next ~ Space ~ SpecificWeekday             ~> ((w) => NextWeekdayByName(w)) |
    Next ~ Space ~ SpecificMonth               ~> ((m) => NextMonthByName(m)) |
    Next ~ Space ~ Weeks                       ~> (()  => InWeeks(1)) |
    Next ~ Space ~ Years                       ~> (()  => InYears(1)) |
    In ~ Space ~ (Count | Number) ~ Space ~ Days         ~> ((d) => InDays(d)) |
    In ~ Space ~ (Count | Number) ~ Space ~ Weeks        ~> ((w) => InWeeks(w)) |
    In ~ Space ~ (Count | Number) ~ Space ~ Months       ~> ((m) => InMonths(m)) |
    In ~ Space ~ (Count | Number) ~ Space ~ Years        ~> ((y) => InYears(y))
  }

  def RelativeDatesPast = rule {
    Last ~ Space ~ Months                      ~> (()  => InMonths(-1)) |
    Last ~ Space ~ SpecificWeekday             ~> ((w) => LastWeekdayByName(w)) |
    Last ~ Space ~ SpecificMonth               ~> ((m) => LastMonthByName(m)) |
    Last ~ Space ~ Weeks                       ~> (()  => InWeeks(-1)) |
    Last ~ Space ~ Years                       ~> (()  => InYears(-1)) |
    Count ~ Space ~ Days ~ Space ~ Ago         ~> ((c) => InDays(-c)) |
    Count ~ Space ~ Weeks ~ Space ~ Ago        ~> ((c) => InWeeks(-c)) |
    Count ~ Space ~ Months ~ Space ~ Ago       ~> ((c) => InMonths(-c)) |
    Count ~ Space ~ Years ~ Space ~ Ago        ~> ((c) => InYears(-c))
  }

  def TimeDurations = rule {
    DurationPrefix ~ Seconds                   ~> (ForSeconds(_)) |
    DurationPrefix ~ Minutes                   ~> (ForMinutes(_)) |
    DurationPrefix ~ Hours                     ~> (ForHours(_)) |
    From ~ Space ~ AbsoluteTimes ~ Space ~ (To | UnTill) ~ Space ~ AbsoluteTimes ~> (FromTimeToTime(_,_)) |
    UnTill ~ Space ~ AbsoluteTimes             ~> (UntilTime(_))
  }

  def DateDurations = rule {
    DurationPrefix ~ Days ~ Space ~ Starting ~ Space ~ RelativeDates ~> ((d, e) => RelativeDateDuration(e,ForDays(d))) |
    DurationPrefix ~ Days                      ~> (ForDays(_)) |
    From ~ Space ~ SpecificWeekday ~ Space ~ To ~ Space ~ SpecificWeekday ~> ((s,f) => RelativeDateDuration(NextWeekdayByName(s),UntilWeekday(f))) |
    UnTill ~ Space ~ SpecificWeekday             ~> ((w) => RelativeDateDuration(InDays(0),UntilWeekday(w)))
  }

  def DurationPrefix = rule {
    For ~ Space ~ Number ~ Space
  }

  def Cardinal     = rule { First | Second | Third | Fourth | Fifth}
  def First        = rule { (ignoreCase("first")  | ignoreCase("1st")) ~> (() => 1)}
  def Second       = rule { (ignoreCase("second") | ignoreCase("2nd")) ~> (() => 2)}
  def Third        = rule { (ignoreCase("third")  | ignoreCase("3rd")) ~> (() => 3)}
  def Fourth       = rule { (ignoreCase("fourth") | ignoreCase("4th")) ~> (() => 4)}
  def Fifth        = rule { (ignoreCase("fifth")  | ignoreCase("5th")) ~> (() => 5)}

  def Count        = rule { One | Two | Three | Four | Five }
  def One          = rule { ignoreCase("one")   ~> (() => 1)}
  def Two          = rule { ignoreCase("two")   ~> (() => 2)}
  def Three        = rule { ignoreCase("three") ~> (() => 3)}
  def Four         = rule { ignoreCase("four")  ~> (() => 4)}
  def Five         = rule { ignoreCase("five")  ~> (() => 5)}

  def Ago          = rule { ignoreCase("ago") }
  def At           = rule { ignoreCase("at") }
  def For          = rule { ignoreCase("for") }
  def From         = rule { ignoreCase("from") }
  def FromNow      = rule { From ~ Space ~ Now }
  def In           = rule { ignoreCase("in") }
  def Now          = rule { ignoreCase("now") }
  def Of           = rule { ignoreCase("of") }
  def On           = rule { ignoreCase("on") }
  def Starting     = rule { ignoreCase("starting") }
  def Till         = rule { ignoreCase("till") }
  def To           = rule { ignoreCase("to") }
  def Th           = rule { ignoreCase("th") }
  def Until        = rule { ignoreCase("until") }
  def UnTill       = rule { Till | Until }

  def Last         = rule { ignoreCase("last") }
  def Next         = rule { ignoreCase("next") }

  def Today        = rule { ignoreCase("today") }
  def Tomorrow     = rule { ignoreCase("tomorrow") }
  def Yesterday    = rule { ignoreCase("yesterday") }

  def Seconds      = rule { ignoreCase("second") ~ optional(ignoreCase("s")) }
  def Minutes      = rule { ignoreCase("minute") ~ optional(ignoreCase("s")) }
  def Hours        = rule { ignoreCase("hour")   ~ optional(ignoreCase("s")) }
  def Days         = rule { ignoreCase("day")    ~ optional(ignoreCase("s")) }
  def Weeks        = rule { ignoreCase("week")   ~ optional(ignoreCase("s")) }
  def Months       = rule { ignoreCase("month")  ~ optional(ignoreCase("s")) }
  def Years        = rule { ignoreCase("year")   ~ optional(ignoreCase("s")) }

  def Noon         = rule { ignoreCase("noon") ~> (() => new Time(12,0))}
  def Afternoon    = rule { ignoreCase("afternoon") ~> (() => new Time(16,0))}
  def Evening      = rule { ignoreCase("evening") ~> (() => new Time(19,0))}
  def Midnight     = rule { ignoreCase("midnight") ~> (() => new Time(0,0))}

  def Number       = rule { capture(Digits) ~> (_.toInt) }
  def Digits       = rule { oneOrMore(CharPredicate.Digit) }

  def Colon        = rule { ":" }
  def Dash         = rule { "-" }
  def Dot          = rule { "." }
  def Slash        = rule { "/" }
  def Space        = rule { zeroOrMore(" ") }

  def FormalDate   = rule {
   DayDigits ~ optional(Dot | Th) ~ Space ~ SpecificMonth ~ Space ~ YearDigits ~> ((d,m,y) => new Date(m.getValue,d,Some(y))) |
   DayDigits ~ optional(Dot | Th) ~ Space ~ SpecificMonth ~> ((d,m) => new Date(m.getValue,d)) |
   Cardinal ~ Space ~ SpecificMonth ~ Space ~ YearDigits ~> ((d,m,y) => new Date(m.getValue,d,Some(y))) |
   Cardinal ~ Space ~ SpecificMonth ~ Space ~> ((d,m) => new Date(m.getValue,d))
  }
  def IsoDate      = rule { YearDigits ~ optional(Dash | Slash) ~ MonthDigits ~ optional(Dash | Slash) ~ DayDigits ~> ((y,m,d) => new Date(m,d, Some(y))) }
  def LittleEndianDate = rule { DayDigits ~ optional(Dash | Slash) ~ MonthDigits ~ optional(Dash | Slash) ~ YearDigits ~> ((d,m,y) => new Date(m,d, Some(y))) }
  def YearDigits   = rule { capture(4.times(CharPredicate.Digit)) ~> (_.toInt) }
  def MonthDigits  = rule { capture("0" ~ CharPredicate.Digit | "1" ~ anyOf("012" )) ~> (_.toInt) }
  def DayDigits    = rule { capture(anyOf("012") ~ CharPredicate.Digit | "3" ~ anyOf("01" )) ~> (_.toInt) }

  def IsoTime      = rule { HourDigits ~ Colon ~ MinuteDigits ~> ((h,m) => new Time(h, m)) }
  def HourDigits   = rule { capture(anyOf("01") ~ optional(CharPredicate.Digit) | ("2" ~ optional(anyOf("01234" ))) | anyOf("3456789")) ~> (_.toInt) }
  def MinuteDigits = rule { capture(anyOf("012345") ~ optional(CharPredicate.Digit)) ~> (_.toInt) }

  def TwelveHourTime = rule {
    TwelveHour ~ Colon ~ MinuteDigits ~ Space ~ Am ~> ((h,m) => new Time(h % 12,m)) |
    TwelveHour ~ Colon ~ MinuteDigits ~ Space ~ Pm ~> ((h,m) => new Time((h % 12) + 12,m)) |
    TwelveHour ~ Am                     ~> ((h) => new Time(h % 12,0)) |
    TwelveHour ~ Pm                     ~> ((h) => new Time((h % 12) + 12,0)) |
    TwelveHour ~ Space ~ Am             ~> ((h) => new Time(h % 12,0)) |
    TwelveHour ~ Space ~ Pm             ~> ((h) => new Time((h % 12) + 12,0))
  }
  def TwelveHour = rule { capture("0" ~ CharPredicate.Digit | "1" ~ anyOf("012") | CharPredicate.Digit) ~> (_.toInt) }
  def Am         = rule { ignoreCase("a") ~ optional (Dot) ~ ignoreCase("m") ~ optional(Dot) }
  def Pm         = rule { ignoreCase("p") ~ optional (Dot) ~ ignoreCase("m") ~ optional(Dot) }

  def SpecificWeekday = rule {Monday | Tuesday | Wednesday | Thursday | Friday | Saturday | Sunday}
  def Monday          = rule {(ignoreCase("monday")    | ignoreCase("mon")) ~> (() => DayOfWeek.MONDAY )}
  def Tuesday         = rule {(ignoreCase("tuesday")   | ignoreCase("tue")) ~> (() => DayOfWeek.TUESDAY )}
  def Wednesday       = rule {(ignoreCase("wednesday") | ignoreCase("wed")) ~> (() => DayOfWeek.WEDNESDAY )}
  def Thursday        = rule {(ignoreCase("thursday")  | ignoreCase("thu")) ~> (() => DayOfWeek.THURSDAY )}
  def Friday          = rule {(ignoreCase("friday")    | ignoreCase("fri")) ~> (() => DayOfWeek.FRIDAY )}
  def Saturday        = rule {(ignoreCase("saturday")  | ignoreCase("sat")) ~> (() => DayOfWeek.SATURDAY )}
  def Sunday          = rule {(ignoreCase("sunday")    | ignoreCase("sun")) ~> (() => DayOfWeek.SUNDAY )}

  def SpecificMonth = rule {January | Febuary | March | April | May | June | July | August | September | October | November | December}
  def January       = rule {(ignoreCase("january")   | ignoreCase("jan")) ~> (() => Month.JANUARY )}
  def Febuary       = rule {(ignoreCase("february")  | ignoreCase("feb")) ~> (() => Month.FEBRUARY )}
  def March         = rule {(ignoreCase("march")     | ignoreCase("mar")) ~> (() => Month.MARCH )}
  def April         = rule {(ignoreCase("april")     | ignoreCase("apr")) ~> (() => Month.APRIL )}
  def May           = rule {(ignoreCase("may"))                           ~> (() => Month.MAY )}
  def June          = rule {(ignoreCase("june")      | ignoreCase("jun")) ~> (() => Month.JUNE )}
  def July          = rule {(ignoreCase("july")      | ignoreCase("jul")) ~> (() => Month.JULY )}
  def August        = rule {(ignoreCase("august")    | ignoreCase("aug")) ~> (() => Month.AUGUST )}
  def September     = rule {(ignoreCase("september") | ignoreCase("sept") | ignoreCase("sep")) ~> (() => Month.SEPTEMBER )}
  def October       = rule {(ignoreCase("october")   | ignoreCase("oct")) ~> (() => Month.OCTOBER )}
  def November      = rule {(ignoreCase("november")  | ignoreCase("nov")) ~> (() => Month.NOVEMBER )}
  def December      = rule {(ignoreCase("december")  | ignoreCase("dec")) ~> (() => Month.DECEMBER )}
}
