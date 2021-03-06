package miniboxing

package object plugin {
  /**
   * A bridge from the old miniboxing annotation to the new one: `scala.miniboxed`.
   * @see [[scala.miniboxed]]
   */
  @deprecated(message = "Please use `scala.miniboxed` instead of `miniboxing.plugin.minispec`.", "0.1-SNAPSHOT")
  type minispec = scala.miniboxed
}
