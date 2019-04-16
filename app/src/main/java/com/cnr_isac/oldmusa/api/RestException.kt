package com.cnr_isac.oldmusa.api

import java.lang.RuntimeException

class RestException(val code: Int, val codeMessage: String, val method: String, val url: String, val responseContent: String?)
    : RuntimeException("$code: '$codeMessage' on '$method' '$url', body: '$responseContent'")
