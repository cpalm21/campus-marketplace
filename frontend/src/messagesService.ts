import { listings } from "./marketplaceData"

export type ChatMessage = {
  id: string
  text: string
  sentAt: string
  isMine: boolean
}

export type Conversation = {
  id: string
  listingId: string
  listingTitle: string
  listingImage: string
  listingPrice: number
  participantName: string
  messages: ChatMessage[]
}

export interface MessagingService {
  listConversations(): Promise<Conversation[]>
  openConversation(listingId: string): Promise<Conversation>
  sendMessage(conversationId: string, text: string): Promise<Conversation>
}

// Temporary demo data. Nothing here sends a message to another account.
// Keep this behind the service so the page can use an API adapter later.
const inboxes = new Map<string, Conversation[]>()

function makeConversation(listingId: string): Conversation {
  const listing = listings.find((item) => item.id === listingId)
  if (!listing) throw new Error("This listing is no longer available.")

  const names: Record<string, string> = {
    "desk-1": "Jordan Lee",
    "biology-books": "Maya Patel",
  }

  return {
    id: crypto.randomUUID(),
    listingId: listing.id,
    listingTitle: listing.title,
    listingImage: listing.image,
    listingPrice: listing.price,
    participantName: names[listing.id] ?? "Demo seller",
    messages: [],
  }
}

function createDemoInbox(): Conversation[] {
  return ["desk-1", "biology-books"]
    .filter((id) => listings.some((listing) => listing.id === id))
    .map((id) => {
      const chat = makeConversation(id)
      chat.messages = [{
        id: crypto.randomUUID(),
        text: id === "desk-1"
          ? "Sample message: the desk is still available."
          : "Sample message: the lab manual is included.",
        sentAt: new Date(Date.now() - 120_000).toISOString(),
        isMine: false,
      }]
      return chat
    })
}

export function createMessagingService(firebaseUid: string): MessagingService {
  // Each signed-in account gets its own demo inbox in this browser tab.
  // Refreshing the page resets the demo. No localStorage or database writes.
  if (!inboxes.has(firebaseUid)) inboxes.set(firebaseUid, createDemoInbox())
  const read = () => inboxes.get(firebaseUid)!

  return {
    async listConversations() {
      return structuredClone(read())
    },

    async openConversation(listingId) {
      const existing = read().find((chat) => chat.listingId === listingId)
      if (existing) return structuredClone(existing)
      const chat = makeConversation(listingId)
      inboxes.set(firebaseUid, [chat, ...read()])
      return structuredClone(chat)
    },

    async sendMessage(conversationId, text) {
      const body = text.trim()
      if (!body) throw new Error("Enter a message first.")
      if (body.length > 2000) throw new Error("Use 2,000 characters or fewer.")
      const chat = read().find((item) => item.id === conversationId)
      if (!chat) throw new Error("This conversation is no longer available.")

      const updated: Conversation = {
        ...chat,
        messages: [...chat.messages, {
          id: crypto.randomUUID(),
          text: body,
          sentAt: new Date().toISOString(),
          isMine: true,
        }],
      }
      inboxes.set(firebaseUid, [updated, ...read().filter((item) => item.id !== chat.id)])
      return structuredClone(updated)
    },
  }
}
