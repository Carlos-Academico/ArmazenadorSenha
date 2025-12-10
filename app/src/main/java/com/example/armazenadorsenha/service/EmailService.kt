package com.example.armazenadorsenha.service

import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {

    // Configurações do seu e-mail de envio
    private const val SENDER_EMAIL = "henrique.queiroz@academico.ifpb.edu.br" // SEU EMAIL DE ENVIO
    private const val SENDER_PASSWORD = "nlpc oggm gqdn wjdv" // SENHA DE APP DO GMAIL ou credencial SMTP
    private const val SMTP_HOST = "smtp.gmail.com" // Exemplo para Gmail
    private const val SMTP_PORT = "587"

    fun sendWelcomeEmail(recipientEmail: String) {
        // Usa uma thread separada para operações de rede
        Thread {
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = SMTP_HOST
                props["mail.smtp.port"] = SMTP_PORT

                // 1. Cria a sessão de e-mail
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                // 2. Cria a mensagem
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
                )
                message.subject = "🥳 Boas-Vindas ao SaveKey - Seu Cofre de Senhas!"

                // 3. Define o conteúdo HTML
                message.setContent(
                    generateHtmlContent(),
                    "text/html; charset=utf-8"
                )

                // 4. Envia a mensagem
                Transport.send(message)
                println("E-mail enviado com sucesso para: $recipientEmail")

            } catch (e: Exception) {
                // Logue o erro para debug
                e.printStackTrace()
                println("Erro ao enviar e-mail: ${e.message}")
            }
        }.start()
    }

    fun sendNewPasswordNotification(recipientEmail: String, serviceTitle: String, username: String) {
        Thread {
            try {
                // ... (Configuração da sessão SMTP, Session.getInstance(props, authenticator)) ...

                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = SMTP_HOST
                props["mail.smtp.port"] = SMTP_PORT

                // 1. Cria a sessão de e-mail
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
                )
                message.subject = "✅ Senha Nova Cadastrada: $serviceTitle"

                // 3. Define o conteúdo HTML
                message.setContent(
                    generateNewPasswordHtml(serviceTitle, username), // Chama o novo template
                    "text/html; charset=utf-8"
                )

                // 4. Envia a mensagem
                Transport.send(message)
                println("Notificação de nova senha enviada para: $recipientEmail")

            } catch (e: Exception) {
                e.printStackTrace()
                println("Erro ao enviar notificação de nova senha: ${e.message}")
            }
        }.start()
    }

    fun generateHtmlContent(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); overflow: hidden; }
                .header { background-color: #1a73e8; color: white; padding: 20px; text-align: center; }
                .content { padding: 30px; line-height: 1.6; color: #333333; }
                .footer { background-color: #eeeeee; padding: 20px; text-align: center; font-size: 12px; color: #777777; }
                .safe-text { color: #d93025; font-weight: bold; margin-top: 20px; border: 1px solid #d93025; padding: 10px; border-radius: 4px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>🎉 Bem-Vindo(a) ao SaveKey!</h2>
                </div>
                <div class="content">
                    <p>Olá,</p>
                    <p>Ficamos muito felizes por você ter escolhido o **SaveKey** para proteger suas informações mais sensíveis. Seu cofre de senhas foi criado com sucesso.</p>
                    
                    <h3>Seu Cofre está Pronto:</h3>
                    <ul>
                        <li><strong>Segurança de Ponta:</strong> Todas as suas senhas serão criptografadas no seu dispositivo com sua Senha Mestra.</li>
                        <li><strong>Login Rápido:</strong> Você pode usar a Impressão Digital (se ativada) para acessar seu cofre rapidamente.</li>
                    </ul>

                    <div class="safe-text">
                        🔒 Lembrete de Segurança: Este e-mail NÃO contém sua Senha Mestra, pois ela NUNCA deve ser transmitida por e-mail. Mantenha-a segura!
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="#" style="background-color: #1a73e8; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;">Acessar o Aplicativo</a>
                    </p>
                </div>
                <div class="footer">
                    <p>SaveKey | Sua segurança, nossa prioridade.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
    }

    fun generateNewPasswordHtml(serviceTitle: String, username: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); overflow: hidden; }
                .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                .content { padding: 30px; line-height: 1.6; color: #333333; }
                .detail-box { background-color: #e9ecef; padding: 15px; border-radius: 4px; margin-top: 15px; }
                .detail-box p { margin: 5px 0; }
                .footer { background-color: #eeeeee; padding: 20px; text-align: center; font-size: 12px; color: #777777; }
                .security-note { color: #d93025; font-weight: bold; margin-top: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Nova Senha Registrada no SaveKey</h2>
                </div>
                <div class="content">
                    <p>Confirmamos que você cadastrou um novo registro em seu cofre de senhas SaveKey.</p>
                    
                    <h3>Detalhes do Registro:</h3>
                    <div class="detail-box">
                        <p><strong>Serviço:</strong> $serviceTitle</p>
                        <p><strong>Usuário:</strong> $username</p>
                    </div>

                    <p style="margin-top: 25px;">Se você não reconhece esta atividade, por favor, acesse o aplicativo imediatamente e verifique a segurança da sua conta.</p>

                    <div class="security-note">
                        Por motivos de segurança, a senha cadastrada NUNCA é enviada por e-mail.
                    </div>
                </div>
                <div class="footer">
                    <p>SaveKey | Sua segurança, nossa prioridade.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
    }
}