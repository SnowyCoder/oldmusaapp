package com.cnr_isac.oldmusa.api

class User(api: Api, id: Long, var username: String, password: String?, var permission: Char) : ApiEntity(api, id) {
    private var _password: String? = password

    val access: List<Site>
        get() = api.getUserAccessIds(id).map { api.getSite(it) }

    /**
     * Rewrites the password with the new one,
     * Use [commit] to apply changes
     *
     * @param newPassword The new User's password
     */
    fun changePassword(newPassword: String) {
        _password = newPassword
    }

    fun addAccess(site: Site) {
        api.addUserAccess(id, site.id)
    }

    fun removeAccess(site: Site) {
        api.removeUserAccess(id, site.id)
    }


    fun onUpdate(data: ApiUser) {
        assert(id == data.id)
        this.username = data.username!!
        this.permission = data.permission!!
        this._password = null
    }

    fun serialize(): ApiUser {
        return ApiUser(id, username, _password, permission)
    }

    fun commit() {
        api.updateUser(id, serialize())
    }

    fun delete() {
        api.deleteUser(id)
    }
}