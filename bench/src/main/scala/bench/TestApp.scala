package bench

import org.openjdk.jmh.infra.Blackhole

object TestApp extends App {

  new ParserBench().smile(
    new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")
  )
}
