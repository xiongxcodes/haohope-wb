package com.wb.controls;

import com.wb.tool.MailSender;

public class Mailer extends Control {
	public void create() throws Exception {
		if (!this.gb("disabled", false)) {
			MailSender mailSender = new MailSender(this.gs("smtp"), this.gs("username"), this.gs("password"),
					this.gb("needAuth", true));

			try {
				mailSender.send(this.gs("from"), this.gs("to"), this.gs("cc"), this.gs("bcc"), this.gs("title"),
						this.gs("content"), this.gs("attachFiles"), this.request, this.gs("attachObjects"),
						this.gs("attachObjectNames"));
			} finally {
				mailSender.close();
			}

		}
	}
}