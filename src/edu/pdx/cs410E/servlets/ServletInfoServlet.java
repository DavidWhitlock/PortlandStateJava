package edu.pdx.cs410E.servlets;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * A servlet that returns all sorts of information available from the
 * servlet API.
 *
 * @author David Whitlock
 * @since Summer 2005
 */
public class ServletInfoServlet extends HttpServlet {

	/**
	 * Display information from the {@link HttpServletRequest}, {@link
	 * HttpServletResponse}
	 */
	protected void doGet(HttpServletRequest request,
											 HttpServletResponse response)
		throws ServletException, IOException {

		response.setContentType("text/html");
		response.setBufferSize(8192);
		PrintWriter out = response.getWriter();

		out.println("<HTML>");
		out.println("<TITLE>Servlet Information</TITLE>");
		out.println("<BODY>");

		out.println("<H1>Servlet Information " + (new Date()) + "</H1>");

		dump(this.getServletConfig().getServletContext(), out);

		dump(this.getServletConfig().getServletContext().getContext("/admin"), out);

		dump(request, out);

		out.println("</BODY>");
		out.println("</HTML>");
	}

	/**
	 * Starts a new HTML table with the given title
	 */
	private static void startTable(String title, PrintWriter out) {
		out.println("<TABLE BORDER=\"1\">");
		out.print("<CAPTION ALIGN=\"top\">");
		out.print(title);
		out.println("</CAPTION>");
	}

	/**
	 * Inserts a row into an HTML table
	 */
	private static void tableRow(String key, Object value, PrintWriter out) {
		out.print("  <TR><TD>");
		out.print(key);
		out.print("</TD><TD>");
		out.print(value);
		out.println("</TD></TR>");
	}

	/**
	 * Ends an HTML table
	 */
	private static void endTable(PrintWriter out) {
		out.println("</TABLE>");
	}

	/**
	 * Dumps information about a <code>HttpServletRequest</code> to the
	 * given <code>PrintWriter</code>.
	 */
	private static void dump(HttpServletRequest request, PrintWriter out) {
		startTable("HttpServletRequest", out);

		for (Enumeration attrs = request.getAttributeNames(); attrs.hasMoreElements(); ) {
			String name = (String) attrs.nextElement();
			Object attr = request.getAttribute(name);
			tableRow("Attribute \"" + name + "\"", attr, out);
		}

		tableRow("Character encoding", request.getCharacterEncoding(), out);
		tableRow("Context length", String.valueOf(request.getContentLength()), out);
		tableRow("Content type", request.getContentType(), out);
		tableRow("Local address", request.getLocalAddr(), out);
		tableRow("Local port", String.valueOf(request.getLocalPort()), out);

		tableRow("Remote address", request.getRemoteAddr(), out);
		tableRow("Remote host", request.getRemoteHost(), out);
		tableRow("Remote port", String.valueOf(request.getRemotePort()), out);

		tableRow("Server name", request.getServerName(), out);
		tableRow("Server port", String.valueOf(request.getServerPort()), out);

		tableRow("Locale", request.getLocale(), out);

		{
			StringBuffer sb = new StringBuffer();
			for (Enumeration locales = request.getLocales(); locales.hasMoreElements(); ) {
				sb.append(locales.nextElement());
				sb.append(" ");
			}
			tableRow("Locales", sb.toString().trim(), out);
		}

		for (Iterator iter = request.getParameterMap().entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object param = entry.getValue();
			if (param instanceof String[]) {
				String[] array = (String[]) param;
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < array.length; i++) {
					sb.append(array[i]);
					if (i < array.length - 1) {
						sb.append(", ");
					}
				}
				param = sb;
			}
			tableRow("Parameter \"" + entry.getKey() + "\"", String.valueOf(param), out);
		}

		tableRow("Protocol", request.getProtocol(), out);
		tableRow("Scheme", request.getScheme(), out);
		tableRow("Is secure?", String.valueOf(request.isSecure()), out);

		// HTTP

		tableRow("Method", request.getMethod(), out);
		tableRow("Auth Type", request.getAuthType(), out);

		for (Enumeration names = request.getHeaderNames(); names.hasMoreElements(); ) {
			String name = (String) names.nextElement();
			String value = request.getHeader(name);
			tableRow("Header \"" + name + "\"", value, out);
		}

		tableRow("Path Info", request.getPathInfo(), out);
		tableRow("Path Translated", request.getPathTranslated(), out);
		tableRow("Query String", request.getQueryString(), out);
		tableRow("Remote User", request.getRemoteUser(), out);
		tableRow("Requested Session ID", request.getRequestedSessionId(), out);
		tableRow("Request URI", request.getRequestURI(), out);
		tableRow("Request URL", request.getRequestURL(), out);
		tableRow("Servlet Path", request.getServletPath(), out);
		tableRow("User Principal", request.getUserPrincipal(), out);

		endTable(out);
	}

	/**
	 * Dumps information about a <code>ServletContext</code> to the
	 * given <code>PrintWriter</code>
	 */
	private static void dump(ServletContext context, PrintWriter out) {
		startTable("ServletContext", out);

		tableRow("Context name", context.getServletContextName(), out);
		tableRow("Server Info", context.getServerInfo(), out);

		for (Enumeration attrs = context.getAttributeNames(); attrs.hasMoreElements(); ) {
			String name = (String) attrs.nextElement();
			Object value = context.getAttribute(name);
			tableRow("Attribute \"" + name + "\"", String.valueOf(value), out);
		}

		for (Enumeration initParams = context.getInitParameterNames(); initParams.hasMoreElements(); ) {
			String name = (String) initParams.nextElement();
			String param = context.getInitParameter(name);
			tableRow("Init parameter \"" + name + "\"", param, out);
		}

		tableRow("Major version", String.valueOf(context.getMajorVersion()), out);
		tableRow("Minor version", String.valueOf(context.getMinorVersion()), out);


		endTable(out);
	}

}