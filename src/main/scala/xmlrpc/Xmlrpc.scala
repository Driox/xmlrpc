package xmlrpc

import akka.util.Timeout
import xmlrpc.protocol._

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq
import play.api.libs.ws.XMLBodyReadables._
import play.api.libs.ws.DefaultBodyWritables._
import XmlrpcProtocol._
import play.api.libs.ws.StandaloneWSClient

/**
  * This is the client api to connect to the Xmlrpc server. A client can send any request
  * and he will receive a response. A request is a method call and a response is the result of
  * that method in the server or a fault.
  *
  * The configuration of the Server is a Uri, make sure you have this implicit in context
  * before calling invokeMethod.
  *
  */
object Xmlrpc {

  case class XmlrpcServer(fullAddress: String)

  def invokeMethod[P: Datatype, R: Datatype](name: String, parameter: P = Void)
                                   (implicit xmlrpcServer: XmlrpcServer,
                                    ws_client:StandaloneWSClient,
                                    ec: ExecutionContext,
                                    fc: Timeout): XmlrpcResponse[R] = {

    val request: NodeSeq = writeXmlRequest(name, parameter)
    val body: String = """<?xml version="1.0"?>""" + request.toString

    try {
      val response = ws_client
        .url(xmlrpcServer.fullAddress)
        .post(body)
        .map(_.body[NodeSeq])
      new XmlrpcResponse[R](response map readXmlResponse[R])
    } catch {
      case t: Throwable => XmlrpcResponse(ConnectionError("An exception has been thrown by Spray", Some(t)).failures)
    }
  }
}
