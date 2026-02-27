package com.github.believepxw.yigo.tool.rest

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.RestService
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import java.awt.Frame
import java.io.File

class OpenIdeaFileRestService : RestService() {

    override fun getServiceName(): String = "yigo/openFile"

    override fun isMethodSupported(method: HttpMethod): Boolean {
        return method == HttpMethod.GET || method == HttpMethod.POST
    }

    override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
        val parameters = urlDecoder.parameters()
        val filePathList = parameters["file"]
        
        if (filePathList.isNullOrEmpty()) {
            val response = io.netty.handler.codec.http.DefaultFullHttpResponse(
                io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
                io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST,
                io.netty.buffer.Unpooled.copiedBuffer("Missing 'file' parameter.", Charsets.UTF_8)
            )
            sendResponse(request, context, response)
            return null
        }

        val filePath = filePathList[0]
        
        ApplicationManager.getApplication().invokeLater {
            val file = File(FileUtil.toSystemDependentName(filePath))
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            
            if (virtualFile != null) {
                val projects = ProjectManager.getInstance().openProjects
                val activeProject = projects.firstOrNull { !it.isDisposed }
                
                if (activeProject != null) {
                    FileEditorManager.getInstance(activeProject).focusedEditor
                    FileEditorManager.getInstance(activeProject).openFile(virtualFile, true)
                    
                    // Bring the IDEA window to the front
                    val frame = WindowManager.getInstance().getFrame(activeProject)
                    if (frame != null) {
                        if (!frame.isActive) {
                            AppIcon.getInstance().requestFocus(frame as Frame)
                        }
                        frame.toFront()
                    }
                }
            }
        }
        
        sendOk(request, context)
        return null
    }
    
    // Required to allow CORS if called from a web page in a browser
    override fun isAccessible(request: io.netty.handler.codec.http.HttpRequest): Boolean {
        return true
    }
}
