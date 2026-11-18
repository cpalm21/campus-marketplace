import { useEffect, useMemo, useRef, useState } from "react"
import type { FormEvent } from "react"
import { Link, useNavigate, useParams } from "react-router"
import { useAuth } from "./authContext"
import { Icon } from "./listingComponents"
import { MarketplaceLayout } from "./marketplaceComponents"
import { createMessagingService } from "./messagesService"
import type { Conversation, MessagingService } from "./messagesService"
import "./browse.css"
import "./messagespage.css"

function errorText(error: unknown) {
  return error instanceof Error ? error.message : "Something went wrong. Please try again."
}

function formatTime(value: string) {
  return new Date(value).toLocaleString([], {
    month: "short", day: "numeric", hour: "numeric", minute: "2-digit",
  })
}

export default function MessagesPage() {
  const { user, signedIn } = useAuth()
  if (!user || !signedIn) return null
  // Remount when the account changes, clearing the previous account's UI.
  return <Inbox key={user.uid} uid={user.uid} />
}

function Inbox({ uid }: { uid: string }) {
  const { conversationId, listingId } = useParams()
  const navigate = useNavigate()
  const service = useMemo(() => createMessagingService(uid), [uid])
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError("")

    async function load() {
      try {
        // /messages/new/:listingId also works after the login redirect.
        if (listingId) {
          const chat = await service.openConversation(listingId)
          if (!cancelled) navigate(`/messages/${chat.id}`, { replace: true })
          return
        }
        const chats = await service.listConversations()
        if (!cancelled) setConversations(chats)
      } catch (cause) {
        if (!cancelled) setError(errorText(cause))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => { cancelled = true }
  }, [service, listingId, navigate, retry])

  const active = conversations.find((chat) => chat.id === conversationId)

  function updateConversation(chat: Conversation) {
    setConversations((current) => [chat, ...current.filter((item) => item.id !== chat.id)])
  }

  return (
    <MarketplaceLayout active="messages">
      <main className="messages-main">
        <div className="browse-container">
          <header className="messages-heading">
            <span>Campus conversations</span>
            <h1>Messages</h1>
            <p>Coordinate a public campus pickup with another student.</p>
          </header>
          <p className="messages-demo-note">
            Demo only: messages stay in this tab until refresh and are not delivered to anyone.
          </p>
          {loading ? <p role="status">Loading conversations...</p> : error ? (
            <div className="messages-status" role="alert">
              <p>{error}</p>
              <button type="button" onClick={() => setRetry((value) => value + 1)}>Try again</button>
              <Link to="/messages">Back to messages</Link>
            </div>
          ) : (
            <div className={`messages-shell ${conversationId ? "chat-open" : ""}`}>
              <aside className="conversation-list" aria-label="Conversations">
                <h2>Conversations</h2>
                {conversations.length === 0 && (
                  <div className="messages-status">
                    <p>No conversations yet.</p>
                    <Link to="/browse">Browse items to message a seller</Link>
                  </div>
                )}
                {conversations.map((chat) => {
                  const last = chat.messages.at(-1)
                  return (
                    <Link
                      key={chat.id}
                      className={`conversation-row ${chat.id === active?.id ? "active" : ""}`}
                      to={`/messages/${chat.id}`}
                      aria-current={chat.id === active?.id ? "page" : undefined}
                    >
                      <img src={chat.listingImage} alt="" />
                      <div>
                        <strong>{chat.participantName}</strong>
                        <span>{chat.listingTitle}</span>
                        <p>{last?.text || "Start the conversation"}</p>
                        {last && <small>{formatTime(last.sentAt)}</small>}
                      </div>
                    </Link>
                  )
                })}
              </aside>
              <section className="chat-panel" aria-label="Active conversation">
                {active ? (
                  <Chat key={active.id} conversation={active} service={service} onSent={updateConversation} />
                ) : (
                  <div className="messages-empty">
                    <Icon name="chat" size={32} />
                    <h2>{conversationId ? "Conversation unavailable" : "Choose a conversation"}</h2>
                    <p>{conversationId
                      ? "This demo conversation may have been cleared by a refresh."
                      : "Select a conversation or open an item and choose Message seller."}</p>
                    <Link to="/messages">View conversations</Link>
                    <Link to="/browse">Browse items</Link>
                  </div>
                )}
              </section>
            </div>
          )}
        </div>
      </main>
    </MarketplaceLayout>
  )
}

function Chat({ conversation, service, onSent }: {
  conversation: Conversation
  service: MessagingService
  onSent: (chat: Conversation) => void
}) {
  const [draft, setDraft] = useState("")
  const [sending, setSending] = useState(false)
  const [error, setError] = useState("")
  const stream = useRef<HTMLDivElement>(null)
  const busy = useRef(false)
  const mounted = useRef(true)

  useEffect(() => {
    mounted.current = true
    return () => { mounted.current = false }
  }, [])

  useEffect(() => {
    if (stream.current) stream.current.scrollTop = stream.current.scrollHeight
  }, [conversation.messages.length])

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!draft.trim() || busy.current) return
    busy.current = true
    setSending(true)
    setError("")
    try {
      const updated = await service.sendMessage(conversation.id, draft)
      onSent(updated)
      if (mounted.current) setDraft("")
    } catch (cause) {
      if (mounted.current) setError(errorText(cause))
    } finally {
      busy.current = false
      if (mounted.current) setSending(false)
    }
  }

  return (
    <>
      <div className="chat-header">
        <Link className="chat-back" to="/messages" aria-label="Back to conversations">
          <Icon name="back" size={20} />
        </Link>
        <span className="chat-avatar"><Icon name="user" /></span>
        <div><strong>{conversation.participantName}</strong><span>CampusLoop conversation</span></div>
      </div>
      <Link className="chat-item-reference" to={`/listings/${conversation.listingId}`}>
        <img src={conversation.listingImage} alt="" />
        <div><small>Talking about</small><strong>{conversation.listingTitle}</strong></div>
        <b>${conversation.listingPrice}</b>
        <Icon name="chevron" size={18} />
      </Link>
      <div className="message-stream" ref={stream} role="log" aria-label="Messages" aria-live="polite">
        {conversation.messages.map((message) => (
          <div className={message.isMine ? "bubble mine" : "bubble"} key={message.id}>
            <p>{message.text}</p>
            <span>{message.isMine ? "You" : conversation.participantName} · {formatTime(message.sentAt)}</span>
          </div>
        ))}
        {conversation.messages.length === 0 && (
          <div className="chat-empty"><strong>Start the conversation</strong><p>Ask about availability or a pickup time.</p></div>
        )}
      </div>
      <form className="message-compose" onSubmit={send}>
        {error && <p className="message-error" role="alert">{error}</p>}
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Write a message..."
          aria-label="Message"
          maxLength={2000}
          disabled={sending}
        />
        <button type="submit" disabled={sending || !draft.trim()}>
          <span>{sending ? "Sending..." : "Send"}</span><Icon name="send" size={17} />
        </button>
      </form>
    </>
  )
}
