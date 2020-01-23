package bench

import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import org.openjdk.jmh.infra.Blackhole

object TestApp extends App {

  FromScala(Seq(1, 2, 3))
  new JmhBench().bytesToCcWeeJackson(
    new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")
  )
}
