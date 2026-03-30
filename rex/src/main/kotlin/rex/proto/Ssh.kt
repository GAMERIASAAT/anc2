package rex.proto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.File

/**
 * Coroutine-friendly SSH client wrapper built on sshj.
 *
 * Supports password and private-key auth, remote command execution,
 * and basic SFTP operations.
 *
 * Note: JVM-only (sshj is not available on Android).
 */
class SshClient private constructor(private val ssh: SSHClient) : AutoCloseable {

    data class ExecResult(val stdout: String, val stderr: String, val exitStatus: Int) {
        val isSuccess: Boolean get() = exitStatus == 0
    }

    /**
     * Execute a single command on the remote host.
     * Returns stdout, stderr, and exit status.
     */
    suspend fun exec(command: String): ExecResult = withContext(Dispatchers.IO) {
        ssh.startSession().use { session ->
            session.exec(command).use { cmd ->
                val stdout = cmd.inputStream.readBytes().toString(Charsets.UTF_8)
                val stderr = cmd.errorStream.readBytes().toString(Charsets.UTF_8)
                cmd.join()
                ExecResult(stdout, stderr, cmd.exitStatus ?: -1)
            }
        }
    }

    /**
     * Download a remote file to a local path via SFTP.
     */
    suspend fun download(remotePath: String, localPath: String) = withContext(Dispatchers.IO) {
        ssh.newSFTPClient().use { sftp ->
            sftp.get(remotePath, localPath)
        }
    }

    /**
     * Upload a local file to a remote path via SFTP.
     */
    suspend fun upload(localPath: String, remotePath: String) = withContext(Dispatchers.IO) {
        ssh.newSFTPClient().use { sftp ->
            sftp.put(localPath, remotePath)
        }
    }

    /**
     * List a remote directory via SFTP.
     */
    suspend fun ls(remotePath: String): List<String> = withContext(Dispatchers.IO) {
        ssh.newSFTPClient().use { sftp ->
            sftp.ls(remotePath).map { it.name }
        }
    }

    override fun close() = runCatching { ssh.close() }.let { Unit }

    companion object {

        /**
         * Connect and authenticate with a password.
         * Uses a promiscuous host-key verifier (accepts any host key) — suitable
         * for penetration testing where the host key is not known in advance.
         */
        suspend fun connectPassword(
            host: String,
            port: Int = 22,
            username: String,
            password: String,
            timeoutMs: Int = 10_000
        ): SshClient = withContext(Dispatchers.IO) {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = timeoutMs
            ssh.timeout = timeoutMs
            ssh.connect(host, port)
            ssh.authPassword(username, password)
            SshClient(ssh)
        }

        /**
         * Connect and authenticate with a private key file (PEM/OpenSSH format).
         */
        suspend fun connectKey(
            host: String,
            port: Int = 22,
            username: String,
            keyPath: String,
            passphrase: String? = null,
            timeoutMs: Int = 10_000
        ): SshClient = withContext(Dispatchers.IO) {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = timeoutMs
            ssh.timeout = timeoutMs
            ssh.connect(host, port)
            val provider: KeyProvider = if (passphrase != null)
                ssh.loadKeys(keyPath, passphrase)
            else
                ssh.loadKeys(keyPath)
            ssh.authPublickey(username, provider)
            SshClient(ssh)
        }

        /**
         * Grab the SSH server banner (identification string) without authenticating.
         * Returns something like "SSH-2.0-OpenSSH_8.9p1 Ubuntu-3ubuntu0.10".
         */
        suspend fun banner(host: String, port: Int = 22, timeoutMs: Int = 5_000): String? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val ssh = SSHClient()
                    ssh.addHostKeyVerifier(PromiscuousVerifier())
                    ssh.connectTimeout = timeoutMs
                    ssh.timeout = timeoutMs
                    ssh.connect(host, port)
                    val transport = ssh.transport
                    val serverVersion = transport.serverVersion
                    ssh.close()
                    serverVersion
                }.getOrNull()
            }
    }
}
