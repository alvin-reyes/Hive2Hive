package org.hive2hive.core.process.share;

import java.security.KeyPair;
import java.security.PublicKey;

import net.tomp2p.peers.PeerAddress;

import org.apache.log4j.Logger;
import org.hive2hive.core.exceptions.GetFailedException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.exceptions.PutFailedException;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.model.FileTreeNode;
import org.hive2hive.core.model.UserProfile;
import org.hive2hive.core.network.data.UserProfileManager;
import org.hive2hive.core.network.messages.direct.BaseDirectMessage;
import org.hive2hive.core.process.ProcessManager;

/**
 * 
 * @author Seppi
 */
public class ShareFolderNotificationMessage extends BaseDirectMessage {

	private static final long serialVersionUID = 2507386739362186163L;
	
	private final static Logger logger = H2HLoggerFactory.getLogger(ShareFolderNotificationMessage.class);
	
	private final PublicKey metaFolderId;
	private final KeyPair domainKey;

	public ShareFolderNotificationMessage(PeerAddress targetAddress, PublicKey metaFolderId, KeyPair domainKey) {
		super(targetAddress);
		this.metaFolderId = metaFolderId;
		this.domainKey = domainKey;
	}

	@Override
	public void run() {
		logger.debug("Received a notification message to update the user profile because a new user entered a shared folder.");

		try {
			UserProfileManager profileManager = networkManager.getSession().getProfileManager();
			int pid = ProcessManager.createRandomPseudoPID();
			UserProfile userProfile = profileManager.getUserProfile(pid, true);

			FileTreeNode fileNode = userProfile.getFileById(metaFolderId);
			
			if (fileNode == null) {
				logger.error("Can't find a file node under the given id (public key).");
				return;
			}

			// TODO this is to restrictive, what about several users sharing one single folder?
			if (fileNode.isShared()) {
				logger.error("Folder is already shared.");
				return;
			}

			// modify
			fileNode.setDomainKeys(domainKey);

			// upload modified profile
			logger.debug("Updating the domain key in the user profile");
			profileManager.readyToPut(userProfile, pid);
		} catch (GetFailedException | PutFailedException | NoSessionException e) {
			logger.error(String.format("Updating user profile failed (new users enters a sharing folder). reason = '%s'", e));
			return;
		}
	}

	@Override
	public boolean checkSignature(byte[] data, byte[] signature, String userId) {
		// TODO fix this verification
		return true;
	}
}
