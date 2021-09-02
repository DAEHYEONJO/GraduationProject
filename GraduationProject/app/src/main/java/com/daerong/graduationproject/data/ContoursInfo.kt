package com.daerong.graduationproject.data

import org.opencv.core.Mat
import org.opencv.core.Rect

data class ContoursInfo(var contour : Mat, var rect : Rect, var cx : Double, var cy : Double, var index: Int = -1)