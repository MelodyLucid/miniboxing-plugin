/*
 * This is a full rewiring process test. It should highlight the
 * exact code rewiring as in the paper.
 * There are two things to care about here:
 *  - finding the most specific parent
 *  - rewiring the constructor with the correct type tags
 */
package miniboxing.tests.compile.inheritance
import miniboxing.plugin.minispec

trait Base1[@minispec B11, @minispec B12]
trait Base2[B21, @minispec B22]
trait Base3[@minispec B31, B32]
trait Base4[B41, B42]

class Derived1[@minispec D11, @minispec D12] extends Base1[D11, D12]     with Base2[D11, D12]     with Base3[D11, D12]     with Base4[D11, D12]
class Derived2[D21, @minispec D22]           extends Base1[D21, D22]     with Base2[D21, D22]     with Base3[D21, D22]     with Base4[D21, D22]
class Derived3[@minispec D31, D32]           extends Base1[D31, D32]     with Base2[D31, D32]     with Base3[D31, D32]     with Base4[D31, D32]
class Derived4[D41, D42]                     extends Base1[D41, D42]     with Base2[D41, D42]     with Base3[D41, D42]     with Base4[D41, D42]
class Derived5[D51]                          extends Base1[D51, Boolean] with Base2[D51, Boolean] with Base3[D51, Boolean] with Base4[D51, Boolean]
class Derived6[@minispec D61]                extends Base1[D61, Boolean] with Base2[D61, Boolean] with Base3[D61, Boolean] with Base4[D61, Boolean]
