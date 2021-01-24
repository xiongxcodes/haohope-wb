package com.wb.tool;

import com.wb.common.Base;
import com.wb.common.Var;
import com.wb.util.FileUtil;
import com.wb.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;

public class MailSender {
	private Session session;
	private Transport transport;

	public MailSender(String smtp, String username, String password, boolean needAuth) throws Exception {
		Properties props = new Properties();
		props.put("mail.smtp.host", smtp);
		props.put("mail.smtp.auth", Boolean.toString(needAuth));
		this.session = Session.getInstance(props, (Authenticator) null);
		this.transport = this.session.getTransport("smtp");

		try {
			this.transport.connect(smtp, username, password);
		} catch (Throwable var7) {
			this.close();
		}

	}

	public void close() throws Exception {
		this.transport.close();
	}

	public void send(String from, String to, String cc, String bcc, String title, String content) throws Exception {
		this.send(from, to, cc, bcc, title, content, (String) null, (HttpServletRequest) null, (String) null,
				(String) null);
	}

	public void send(String from, String to, String cc, String bcc, String title, String content, String attachFiles,
			HttpServletRequest request, String attachObjects, String attachObjectNames) throws Exception {
		Multipart multipart = new MimeMultipart();
		MimeMessage message = new MimeMessage(this.session);
		int sepPos = from.indexOf(60);
		if (sepPos != -1) {
			message.setFrom(new InternetAddress(from.substring(sepPos + 1, from.length() - 1),
					from.substring(0, sepPos).trim()));
		} else {
			message.setFrom(new InternetAddress(from));
		}

		message.setRecipients(RecipientType.TO, InternetAddress.parse(to));
		if (!StringUtil.isEmpty(cc)) {
			message.setRecipients(RecipientType.CC, InternetAddress.parse(cc));
		}

		if (!StringUtil.isEmpty(bcc)) {
			message.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc));
		}

		message.setSubject(title);
		message.setSentDate(new Date());
		this.addContent(multipart, content);
		this.attachFiles(multipart, attachFiles, request, attachObjects, attachObjectNames);
		message.setContent(multipart);
		message.saveChanges();
		this.transport.sendMessage(message, message.getAllRecipients());
	}

	private void addContent(Multipart multipart, String content) throws Exception {
		BodyPart bodyPart = new MimeBodyPart();
		bodyPart.setContent(content, "text/html;charset=utf-8");
		multipart.addBodyPart(bodyPart);
	}

	private void attachFiles(Multipart multipart, String attachFiles, HttpServletRequest request, String attachObjects,
			String attachObjectNames) throws Exception {
		int i;
		int j;
		MimeBodyPart bodyPart;
		JSONArray list;
		if (!StringUtil.isEmpty(attachFiles)) {
			list = new JSONArray(attachFiles);
			j = list.length();

			for (i = 0; i < j; ++i) {
				bodyPart = new MimeBodyPart();
				String file = list.getString(i);
				bodyPart.setDataHandler(new DataHandler(new FileDataSource(new File(Base.path, file))));
				bodyPart.setFileName(MimeUtility.encodeText(FileUtil.getFilename(file)));
				bodyPart.setHeader("content-id", "attach" + i);
				multipart.addBodyPart(bodyPart);
			}
		}

		if (!StringUtil.isEmpty(attachObjects)) {
			list = new JSONArray(attachObjects);
			boolean hasObjNames = !StringUtil.isEmpty(attachObjectNames);
			JSONArray objNames;
			if (hasObjNames) {
				objNames = new JSONArray(attachObjectNames);
			} else {
				objNames = null;
			}

			j = list.length();

			for (i = 0; i < j; ++i) {
				String name = list.getString(i);
				Object object = request.getAttribute(name);
				if (object != null) {
					BinDataSource dataSource;
					if (object instanceof InputStream) {
						dataSource = new BinDataSource((InputStream) object);
					} else if (object instanceof byte[]) {
						dataSource = new BinDataSource((byte[]) object);
					} else {
						dataSource = new BinDataSource(object.toString());
					}

					bodyPart = new MimeBodyPart();
					bodyPart.setDataHandler(new DataHandler(dataSource));
					if (hasObjNames) {
						bodyPart.setFileName(MimeUtility.encodeText(objNames.getString(i)));
					} else {
						bodyPart.setFileName(MimeUtility.encodeText(name));
					}

					bodyPart.setHeader("content-id", name);
					multipart.addBodyPart(bodyPart);
				}
			}
		}

	}
	
	private class BinDataSource implements DataSource {
		private byte[] byteData;

		public BinDataSource(InputStream stream) throws IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			int ch;
			while ((ch = stream.read()) != -1) {
				os.write(ch);
			}

			this.byteData = os.toByteArray();
		}

		public BinDataSource(byte[] data) {
			this.byteData = data;
		}

		public BinDataSource(String data) throws Exception {
			String charset = Var.getString("sys.locale.mailCharset");
			if (StringUtil.isEmpty(charset)) {
				this.byteData = data.getBytes();
			} else {
				this.byteData = data.getBytes(charset);
			}

		}

		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.byteData);
		}

		public OutputStream getOutputStream() throws IOException {
			return null;
		}

		public String getContentType() {
			return "application/octet-stream";
		}

		public String getName() {
			return "dummy";
		}
	}
}