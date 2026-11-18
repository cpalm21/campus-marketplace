import { loadListing } from "./marketplaceData"

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

// Reads the real listing this conversation is about. The seeded sample
// conversations that used to live here pointed at the hardcoded listing ids
// ("desk-1" and friends) that the marketplace no longer has, so the inbox now
// starts empty and fills from the Message seller button on a real listing.
//
// The backend deliberately leaves seller identity out of a listing response,
// so there is no name to show yet. Naming the other person needs either a
// messaging API or a seller display name on the listing.
async function makeConversation(listingId: string): Promise<Conversation> {
  const { listing } = await loadListing(listingId)

  return {
    id: crypto.randomUUID(),
    listingId: listing.id,
    listingTitle: listing.title,
    listingImage: listing.image,
    listingPrice: listing.price,
    participantName: "Seller",
    messages: [],
  }
}

export function createMessagingService(firebaseUid: string): MessagingService {
  // Each signed-in account gets its own demo inbox in this browser tab.
  // Refreshing the page resets the demo. No localStorage or database writes.
  if (!inboxes.has(firebaseUid)) inboxes.set(firebaseUid, [])
  const read = () => inboxes.get(firebaseUid)!

  return {
    async listConversations() {
      return structuredClone(read())
    },

    async openConversation(listingId) {
      const existing = read().find((chat) => chat.listingId === listingId)
      if (existing) return structuredClone(existing)
      const chat = await makeConversation(listingId)
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
