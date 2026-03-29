package anc.core.post

import anc.core.AncModule
import anc.core.ModuleType
import anc.core.session.Session

abstract class Post : AncModule() {
    override val moduleType = ModuleType.POST

    protected val session: Session? get() {
        val sid = datastore.getInt("SESSION", -1)
        return if (sid >= 0) framework.sessionManager.get(sid) else null
    }
}
