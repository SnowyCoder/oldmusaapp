package it.cnr.oldmusa.api.graphql

import com.apollographql.apollo.response.CustomTypeAdapter
import com.apollographql.apollo.response.CustomTypeValue
import java.util.*

class NaiveDateTimeConverter : CustomTypeAdapter<Date> {
    override fun encode(value: Date): CustomTypeValue<Number> {
        return CustomTypeValue.GraphQLNumber(value.time / 1000.0)
    }

    override fun decode(value: CustomTypeValue<*>): Date {
        val v = value.value

        val n = if (v is Number) {
            v.toLong()
        } else {
            v.toString().toLong()
        }


        return Date(n * 1000)
    }

}