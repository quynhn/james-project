package org.apache.james.mailbox.caching;
import java.util.List;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;

/**
 * A MailboxMapper implementation that uses a MailboxByPathCache to cache the information
 * from the underlying MailboxMapper
 * 
 * @param <Id>
 */

public class CachingMailboxMapper<Id extends MailboxId> implements MailboxMapper<Id> {

	private final MailboxMapper<Id> underlying;
	private final MailboxByPathCache<Id> cache;

	public CachingMailboxMapper(MailboxMapper<Id> underlying, MailboxByPathCache<Id> cache) {
		this.underlying = underlying;
		this.cache = cache;
	}
	
	@Override
	public void endRequest() {
		underlying.endRequest();		
	}

	@Override
	public <T> T execute(Transaction<T> transaction) throws MailboxException {
		return underlying.execute(transaction);
	}

	@Override
	public void save(Mailbox<Id> mailbox) throws MailboxException {
		invalidate(mailbox);
		underlying.save(mailbox);
	}

	@Override
	public void delete(Mailbox<Id> mailbox) throws MailboxException {
		invalidate(mailbox);
		underlying.delete(mailbox);
	}

	@Override
	public Mailbox<Id> findMailboxByPath(MailboxPath mailboxName)
			throws MailboxException, MailboxNotFoundException {
		try {
			return cache.findMailboxByPath(mailboxName, underlying);
		} catch (MailboxNotFoundException e) {
			cache.invalidate(mailboxName);
			throw e;
		}
	}

	@Override
	public List<Mailbox<Id>> findMailboxWithPathLike(MailboxPath mailboxPath)
			throws MailboxException {
		// TODO possible to meaningfully cache it?
		return underlying.findMailboxWithPathLike(mailboxPath);
	}

	@Override
	public boolean hasChildren(Mailbox<Id> mailbox, char delimiter)
			throws MailboxException, MailboxNotFoundException {
		// TODO possible to meaningfully cache it?
		return underlying.hasChildren(mailbox, delimiter);
	}

	@Override
	public List<Mailbox<Id>> list() throws MailboxException {
		// TODO possible to meaningfully cache it? is it used at all?
		return underlying.list();
	}

	@Override
	public void updateACL(Mailbox<Id> mailbox, MailboxACL.MailboxACLCommand mailboxACLCommand) throws MailboxException {
		mailbox.setACL(mailbox.getACL().apply(mailboxACLCommand));
	}

	private void invalidate(Mailbox<Id> mailbox) {
		cache.invalidate(mailbox);
	}

	@Override
    public MailboxAnnotation getMetadata(Mailbox<Id> mailbox) throws MailboxException {
        return mailbox.getAnnotation();
    }

    @Override
    public void setMetadata(Mailbox<Id> mailbox, MailboxAnnotation.MailboxAnnotationCommand mailboxAnnotationCommand) throws MailboxException {
        mailbox.setAnnotation(mailbox.getAnnotation().update(mailboxAnnotationCommand));
    }

}
