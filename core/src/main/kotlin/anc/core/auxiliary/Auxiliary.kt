package anc.core.auxiliary

import anc.core.AncModule
import anc.core.ModuleType

abstract class Auxiliary : AncModule() {
    override val moduleType = ModuleType.AUXILIARY
}
