package org.dthv.linkforge.app.annotations

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ResponseBody
import java.lang.annotation.*


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotVersioning()
