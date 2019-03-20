package com.cnr_isac.oldmusa.api

import java.lang.RuntimeException

class RestException(code: Int, codeMessage: String, method: String, url: String, responseContent: String?)
    : RuntimeException("$code: '$codeMessage' on '$method' '$url', body: '$responseContent'")
