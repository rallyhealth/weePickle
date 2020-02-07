package bench

import org.openjdk.jmh.infra.Blackhole

object TestApp extends App {

  val bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")
  new ParserBench().smile
}
