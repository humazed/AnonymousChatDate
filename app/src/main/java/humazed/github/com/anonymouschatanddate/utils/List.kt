package humazed.github.com.anonymouschatanddate.utils

import java.util.*

/**
 * User: YourPc
 * Date: 12/16/2017
 */

/**
 * Returns a random element.
 */
fun <E> List<E>.random(): E? = if (size > 0) get(Random().nextInt(size)) else null

/**
 * Returns a random element using the specified [random] instance as the source of randomness.
 */
fun <E> List<E>.random(random: java.util.Random): E? = if (size > 0) get(random.nextInt(size)) else null
