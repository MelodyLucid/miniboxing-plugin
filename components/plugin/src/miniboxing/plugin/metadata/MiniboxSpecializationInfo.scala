package miniboxing.plugin
package metadata

import scala.collection.mutable

trait MiniboxSpecializationInfo {
  self: MiniboxDuplComponent =>

  import global._
  import definitions._

  /**
   * This class should be extended by various classes containing information
   * about different types of methods that are created during specialization.
   */
  sealed class MethodInfo

  /**
   * The symbol with this information needs a body that forwards
   * to `method`.
   *
   * E.g. `apply` forwards to `apply$mcII$sp` in `Function1$mcII$sp`.
   */
  case class ForwardTo(base: Symbol)(overrider: Boolean) extends MethodInfo {
    override def toString =
      if (overrider)
        "is an override which forwards to the specialized member"
      else
        "is a forwarder to the specialized member"
  }

  /*
   * We need to record how the parameters and the return value should be casted.
   */
  sealed abstract class CastInfo
  case class CastMiniboxToBox(tag: Symbol) extends CastInfo
  case class CastBoxToMinibox(tag: Symbol) extends CastInfo
  case object NoCast extends CastInfo
  case object AsInstanceOfCast extends CastInfo

  /**
   * For the following example:
   *
   *  class IntFun extends Function1[Int, Int] {
   *    def apply(x: Int): Int = ..
   *  }
   *
   *  method `apply` will have type `(Any)Any` and we want in fact to override
   *  `apply$mcII$sp` of type `(Int)Int`, and `apply` will forward to it.
   *
   *  So, if the symbol is `apply`, the `method` will be `apply$mcII$sp`.
   */
  case class OverrideOfSpecializedMethod(method: Symbol) extends MethodInfo {
    override def toString = "is the override of a specialized method: " + method
  }

  /**
   * In the specialized class the function which will have the implementation
   * will be the specialized one and the generic one will forward to it.
   *
   * E.g. `apply$mcII$sp` uses as implementation the body of `apply` which
   * forwards to it. So, `method` will be `apply`.
   */
  case class SpecializedImplementationOf(member: Symbol) extends MethodInfo {
    templateMembers += member
    override def toString = "is a specialized implementation of " + member
  }

  /**
   * Method to access `field`, in a specialized class should be rewired to the
   * actual field.
   */
  case class FieldAccessor(field: Symbol) extends MethodInfo {
    override def toString = "is a setter or getter for " + field
  }

  /**
   * When the newly introduced symbol is abstract and does not
   * have an implementation at all.
   */
  case class Interface() extends MethodInfo {
    override def toString = "is an interface method"
  }

  /**
   * When the newly introduced symbol is abstract and does not
   * have an implementation at all.
   */
  case class DeferredTypeTag(tparam: Symbol) extends MethodInfo {
    override def toString = "is a type tag"
  }

  /**
   * When the newly introduced symbol is abstract and does not
   * have an implementation at all.
   */
  case class DeferredTypeTagImplementation(tparam: Symbol) extends MethodInfo {
    override def toString = "is a type tag from an inherited trait"
  }

  /**
   * While running the `MiniboxInfoTransform` we record information about how
   * the newly created methods should be implemented when reached by the
   * `MiniboxTreeTransformation`.
   */
  object memberSpecializationInfo extends mutable.HashMap[Symbol, MethodInfo] {
    def hasInfo(defn: Tree) = memberSpecializationInfo.this isDefinedAt defn.symbol
  }

  /**
   * The set of members that provide the template to copy and specialize
   * by the specialized overloads
   */
  val templateMembers = mutable.Set[Symbol]()

  /**
   * Every time we create a specialized class (or the interface) we clone
   * the type parameters from the original class. This mapping records
   * how the new params correspond to the old ones.
   */
  type ParamMap = Map[Symbol, Symbol]

  object ParamMap {
    def apply(oldParams: List[Symbol], newOwner: Symbol): ParamMap = {
      // TODO: Enable this:
      // def newName(p: Symbol): Name = if (p.hasAnnotation(MinispecClass)) p.name.append("sp") else p.name
      def newName(p: Symbol): Name = p.name.append("sp")
      val newParams = oldParams map (p => p.cloneSymbol(newOwner, p.flags, newName(p)))

      // Update references to old type parameters to the new type parameters
      // See https://github.com/miniboxing/miniboxing-plugin/issues/36 for details.
      newParams.map(_.modifyInfo(info => info.substituteSymbols(oldParams, newParams)))

      newParams foreach (p => { p.removeAnnotation(MinispecClass); p.removeAnnotation(SpecializedClass) })
      (oldParams zip newParams).toMap
    }
  }

  val originalTraitFlag = mutable.Set.empty[Symbol]

  val specializedBase = mutable.Set.empty[Symbol]

  /**
   * `specializedClass(C)(T1->Long, T2->AnyRef)` gives the info of the specialized
   * version of `C` w.r.t. that environment.
   */
  val specializedClasses =
    new mutable.HashMap[Symbol, mutable.HashMap[PartialSpec, Symbol]] withDefaultValue (mutable.HashMap())

  /**
   * `baseClass` is the inverse of specializedClass: for each specialized class it
   * indicates the base abstract class all specialized classes extend from
   */
  val baseClass =
    new mutable.HashMap[Symbol, Symbol]


  /**
   * Type environment of a class:
   * Needed by the duplicator to replace the symbols in the old tree.
   */
  val typeEnv = new mutable.HashMap[Symbol, TypeEnv]

  /**
   * Partial specialization corresponding to a class.
   */
  val partialSpec = new mutable.HashMap[Symbol, PartialSpec]
  val normalSpec = new mutable.HashMap[Symbol, PartialSpec]

  /**
   * Records for each of the specialized classes the tag field to type parameter
   * correspondence. These are local type tags, used in all members.
   */
  val globalTypeTags = new mutable.HashMap[Symbol, Map[Symbol, Symbol]]

  /**
   * Records for each of the specialized classes the tag field to type parameter
   * correspondence. These are local type tags, used in each member.
   */
  val localTypeTags = new mutable.HashMap[Symbol, Map[Symbol, Symbol]]

  /**
   * `defferredTypeTags` keeps a list of members that represent type tags
   * in a trait -- unlike type tags in a class, which are fields, these are
   * methods which the inheriting class overrides
   */
  val inheritedDeferredTypeTags = new mutable.HashMap[Symbol, mutable.Map[Symbol, Symbol]]
  val primaryDeferredTypeTags = new mutable.HashMap[Symbol, mutable.Map[Symbol, Symbol]]

  /**
   * For each method of the original class and each partial specialization
   * we keep track of the overload specialized for that representation.
   */
  val overloads = new mutable.HashMap[Symbol, mutable.HashMap[PartialSpec, Symbol]]
  val normalizations = new mutable.HashMap[Symbol, mutable.HashMap[PartialSpec, Symbol]]

  /**
   * Which of the members are base (do not take any type tags)
   * TODO: Transform into a set
   */
  val base = new mutable.HashMap[Symbol, Symbol]
  val normbase = new mutable.HashMap[Symbol, Symbol]

  /**
   * Map from original type parameters to new type parameters
   */
  val typeParamMap = new mutable.HashMap[Symbol, ParamMap]

  /**
   * A list of dummy constructors necessary to satisfy the duplicator
   */
  val dummyConstructors = mutable.Set[/* dummy constructor */ Symbol]()
}

