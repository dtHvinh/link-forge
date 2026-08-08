package org.dthv.linkforge.app.exceptions

import org.springframework.dao.DataAccessException

class LinkStorageException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: DataAccessException) : super(message, cause)
}