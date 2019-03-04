package com.example.allonpg.oldmusa.api


class Room(api: Api, id: Long, val museumId: Long, var name: String?) : ApiEntity(api, id) {
    fun onUpdate(data: ApiRoom) {
        assert(id == data.id)
        assert(museumId == data.museumId)
        this.name = data.name
    }

    fun serialize(): ApiRoom {
        return ApiRoom(id, museumId, name)
    }

    fun commit() {
        api.updateRoom(id, serialize())
    }

    fun delete() {
        api.deleteRoom(id)
    }
}