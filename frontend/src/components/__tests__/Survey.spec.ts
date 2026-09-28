import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import axios from 'axios'
import Survey from '../Survey.vue'

enableAutoUnmount(afterEach)

vi.mock('axios')

const mockMenuItems = [
  {
    id: 101,
    name: 'Chicken Alfredo',
    category: 'PASTA',
    price: 220,
    imageName: 'chicken_alfredo.jpg',
    description: 'Custom Chicken Alfredo description from the menu record.',
  },
  { id: 102, name: 'Aglio e Olio', category: 'PASTA', price: 180, imageName: null },
  { id: 103, name: 'Carbonara', category: 'PASTA', price: 210, imageName: null },
  { id: 104, name: 'Spaghetti', category: 'PASTA', price: 190, imageName: null },
  { id: 105, name: 'Tomato Pesto', category: 'PASTA', price: 195, imageName: null },
  { id: 106, name: 'Tuna Pesto', category: 'PASTA', price: 200, imageName: null },
  { id: 107, name: 'Bacon Sandwich', category: 'SANDWICH & WRAPS', price: 150, imageName: null },
  { id: 108, name: 'Ham Sandwich', category: 'SANDWICH & WRAPS', price: 140, imageName: null },
  { id: 109, name: 'Classic Waffle', category: 'WAFFLE', price: 120, imageName: null },
  { id: 110, name: 'Hot Americano', category: 'CLASSICS', price: 110, imageName: null },
  { id: 111, name: 'Iced Americano', category: 'CLASSICS', price: 120, imageName: null },
  { id: 112, name: 'Hot Caffe Latte', category: 'CLASSICS', price: 130, imageName: null },
  { id: 113, name: 'Hot Matcha', category: 'CEREMONIAL MATCHA', price: 160, imageName: null },
  { id: 114, name: 'Blue Ocean', category: 'REFRESHER', price: 130, imageName: null },
  { id: 115, name: 'Chocolate', category: 'SPECIALTY', price: 140, imageName: null },
]

const mockQuestions = [
  {
    id: 1,
    text: 'Which emotion or physical state most strongly makes you want to order this item?',
    questionType: 'RADIO',
  },
  {
    id: 2,
    text: 'In what weather condition does this item feel most satisfying?',
    questionType: 'RADIO',
  },
]

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 20))

describe('Survey.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.scrollTo = vi.fn()
    vi.spyOn(Math, 'random').mockReturnValue(0.99999)

    ;(axios.get as any).mockImplementation((url: string) => {
      if (url.startsWith('/menu-items')) {
        return Promise.resolve({ data: [...mockMenuItems] })
      }
      if (url === '/questions/all') {
        return Promise.resolve({ data: [...mockQuestions] })
      }
      if (url === '/api/stats/survey-status') {
        return Promise.resolve({ data: { isFull: false } })
      }
      return Promise.resolve({ data: [] })
    })
    ;(axios.post as any).mockResolvedValue({
      data: { message: 'Category saved successfully!' },
    })
    localStorage.clear()
    document.body.innerHTML = ''
  })

  it('renders welcome screen initially', () => {
    const wrapper = mount(Survey)
    expect(wrapper.text()).toContain('Welcome to the Food Preference Survey!')
    expect(wrapper.find('.welcome-screen').exists()).toBe(true)
  })

  it('preloads upcoming photos before moving to the next item', async () => {
    const requested: string[] = []
    const originalImage = globalThis.Image
    vi.stubGlobal(
      'Image',
      class {
        decoding = ''
        onerror: (() => void) | null = null
        decode = () => Promise.resolve()
        set src(path: string) {
          requested.push(path)
        }
      },
    )
    ;(axios.get as any).mockImplementation((url: string) =>
      Promise.resolve({
        data:
          url.startsWith('/menu-items')
            ? mockMenuItems
                .slice(0, 5)
                .map((item, index) => ({ ...item, imageName: `photo-${index}.webp` }))
            : [],
      }),
    )

    try {
      const wrapper = mount(Survey)
      await flushPromises()
      expect(requested).toEqual([
        '/items/previews/photo-0.webp',
        '/items/previews/photo-1.webp',
        '/items/previews/photo-2.webp',
      ])

      ;(wrapper.vm as any).nextItem()
      expect(requested).toEqual([
        '/items/previews/photo-0.webp',
        '/items/previews/photo-1.webp',
        '/items/previews/photo-2.webp',
        '/items/previews/photo-3.webp',
      ])
    } finally {
      vi.stubGlobal('Image', originalImage)
    }
  })

  it('starts survey when button is clicked and shows Section 1 Privacy Notice', async () => {
    const wrapper = mount(Survey)
    expect(wrapper.find('.welcome-screen').exists()).toBe(true)

    const startButton = wrapper.find('.primary-btn.pulse')
    await startButton.trigger('click')

    expect(wrapper.find('.welcome-screen').exists()).toBe(false)
    expect(wrapper.find('.app-container').exists()).toBe(true)
    expect(wrapper.find('.privacy-view').exists()).toBe(true)
    expect(wrapper.text()).toContain('SECTION 1 — Privacy Notice & Consent')
    expect(wrapper.text()).toContain('Philippine Data Privacy Act of 2012')
    expect(wrapper.text()).toContain('Republic Act No. 10173')
    expect(wrapper.text()).toContain('understanding dining preferences, evaluating food cravings, and improving our cafe menu items')
    expect(wrapper.text()).toContain('Participation is voluntary')

    // Proceed button should be disabled until consent is checked
    const proceedBtn = wrapper.find('.privacy-proceed-btn')
    expect(proceedBtn.attributes('disabled')).toBeDefined()

    // Toggle consent
    await wrapper.find('.consent-card').trigger('click')
    expect(proceedBtn.attributes('disabled')).toBeUndefined()

    // Click proceed to advance to Section 2
    await proceedBtn.trigger('click')
    expect(wrapper.find('.demographic-view').exists()).toBe(true)
    expect(wrapper.text()).toContain('SECTION 2 — Respondent Information')

    // Can navigate back to Section 1
    const backBtn = wrapper.find('.demo-back-btn')
    await backBtn.trigger('click')
    expect(wrapper.find('.privacy-view').exists()).toBe(true)
  })

  it('shows Section 3 instructions as a modal atop the rating view', async () => {
    const wrapper = mount(Survey)
    await flushPromises()
    await wrapper.find('.primary-btn.pulse').trigger('click')

    // Accept Privacy Notice (Section 1)
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')

    expect(wrapper.find('.demographic-view').exists()).toBe(true)
    const proceedBtn = wrapper.find('.demo-proceed-btn')
    expect(proceedBtn.attributes('disabled')).toBeDefined()

    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')

    expect(proceedBtn.attributes('disabled')).toBeUndefined()
    await proceedBtn.trigger('click')

    // Rating view renders immediately with the instructions modal on top of it
    expect(wrapper.find('.rating-view').exists()).toBe(true)
    const instructionsModal = document.body.querySelector('.instructions-modal-card')
    expect(instructionsModal).not.toBeNull()
    const starterText = instructionsModal?.textContent || ''

    expect(starterText).toContain('SECTION 3 — Menu Item Evaluation')
    expect(starterText).toMatch(/You will be asked to evaluate \d+ menu items\./)
    expect(starterText).toContain(
      'For each menu item, rate how suitable you think the item is for each mood and weather condition.',
    )
    expect(starterText).toContain('Rating scale:')
    expect(starterText).toContain('1 — Not Suitable')
    expect(starterText).toContain('2 — Slightly Suitable')
    expect(starterText).toContain('3 — Moderately Suitable')
    expect(starterText).toContain('4 — Suitable')
    expect(starterText).toContain('5 — Very Suitable')

    const startSec3Btn = document.body.querySelector(
      '.instructions-modal-card .starter-proceed-btn',
    ) as HTMLButtonElement
    expect(startSec3Btn).not.toBeNull()
    startSec3Btn.click()
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(document.body.querySelector('.instructions-modal-card')).toBeNull()
    expect(wrapper.find('.rating-view').exists()).toBe(true)

    const viewInstructionsBtn = wrapper.find('.view-instructions-btn')
    expect(viewInstructionsBtn.exists()).toBe(true)
    await viewInstructionsBtn.trigger('click')
    await wrapper.vm.$nextTick()
    expect(document.body.querySelector('.instructions-modal-card')).not.toBeNull()
    // Rating view stays mounted behind the reopened modal
    expect(wrapper.find('.rating-view').exists()).toBe(true)
  })

  it('allows going back to Section 2 from the instructions modal', async () => {
    const wrapper = mount(Survey)
    await wrapper.find('.primary-btn.pulse').trigger('click')

    // Complete Section 1
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')

    // Complete Section 2
    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')

    expect(document.body.querySelector('.instructions-modal-card')).not.toBeNull()
    expect(wrapper.find('.rating-view').exists()).toBe(true)

    const backBtn = document.body.querySelector(
      '.instructions-modal-actions .nav-btn.secondary',
    ) as HTMLButtonElement
    expect(backBtn).not.toBeNull()
    backBtn.click()
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(document.body.querySelector('.instructions-modal-card')).toBeNull()
    expect(wrapper.find('.demographic-view').exists()).toBe(true)
  })

  it('renders Section 3 Menu Item Evaluation with Question 1 (Mood) & Question 2 (Weather)', async () => {
    const wrapper = mount(Survey)
    await flushPromises()

    await wrapper.find('.primary-btn.pulse').trigger('click')

    // Complete Section 1
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')

    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')

    // Dismiss the instructions modal to interact with the rating view
    ;(wrapper.vm as any).showInstructionsModal = false
    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(wrapper.find('.rating-view').exists()).toBe(true)

    // Verify Menu Item 1 display
    expect(wrapper.find('.item-tag-pill').text()).toContain('MENU ITEM 1')
    expect(wrapper.text()).toContain('Chicken Alfredo')
    expect(wrapper.find('.desc-text').text()).toBe(
      'Custom Chicken Alfredo description from the menu record.',
    )

    // Verify Question 1 — Mood Association
    expect(wrapper.text()).toContain('Question 1 — Mood Association')
    expect(wrapper.text()).toContain(
      'How suitable is Chicken Alfredo for each of the following moods?',
    )

    const moodLabels = [
      'Relaxation (Wants to unwind, destress, or enjoy a calm and peaceful moment)',
      'Focus (Wants to concentrate, study, work, or stay mentally alert)',
      'Celebrate (Marking a milestone, special occasion, reward, or personal achievement)',
      'Comfort (Seeking warmth, familiar flavors, emotional solace, or a cozy feel)',
      'Welcoming (Feeling invited, at ease, warmly received, or creating a hospitable atmosphere)',
      'Socialize (Sharing meals, gathering with friends, family, or colleagues for conversation)',
      'Enjoyment (Savoring pure taste, indulgence, pleasure, and culinary satisfaction)',
    ]

    moodLabels.forEach((mood) => {
      expect(wrapper.text()).toContain(mood)
    })

    // Verify Question 2 — Weather Association
    expect(wrapper.text()).toContain('Question 2 — Weather Association')
    expect(wrapper.text()).toContain(
      'How suitable is Chicken Alfredo for each of the following weather conditions?',
    )

    const weatherLabels = [
      'Rainy (Wet, gloomy, or rainy days; craving something warming, cozy, or comforting)',
      'Hot Dry (High daytime heat with low humidity; craving refreshing, thirst-quenching options)',
      'Cool Dry (Breezy, mild weather, air-conditioned spaces, or cool evening breezes)',
    ]

    weatherLabels.forEach((weather) => {
      expect(wrapper.text()).toContain(weather)
    })

    // Check "Require a response in each row." notices
    const reqFooters = wrapper.findAll('.grid-req-footer')
    expect(reqFooters.length).toBe(2)
    expect(reqFooters[0].text()).toContain('Require a response in each row.')
    expect(reqFooters[1].text()).toContain('Require a response in each row.')

    // Next item button should be disabled because not all rows are answered
    const nextBtn = wrapper.find('.nav-btn.primary')
    expect(nextBtn.attributes('disabled')).toBeDefined()
  })

  it('enforces row requirements, advances to next item dynamically, and submits successfully', async () => {
    const wrapper = mount(Survey)
    await flushPromises()

    // Start Survey -> Complete Section 1 -> Complete Section 2 -> Dismiss instructions modal -> Section 3
    await wrapper.find('.primary-btn.pulse').trigger('click')
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')
    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')
    ;(wrapper.vm as any).showInstructionsModal = false
    await wrapper.vm.$nextTick()
    await flushPromises()

    // Answer all 7 mood rows on Item 1 (Chicken Alfredo)
    const tables = wrapper.findAll('.matrix-table')
    expect(tables.length).toBe(2)

    const moodRows = tables[0].findAll('tbody tr')
    expect(moodRows.length).toBe(7)
    for (const row of moodRows) {
      const cells = row.findAll('.matrix-td')
      await cells[3].trigger('click') // Select rating 4 (Suitable)
    }

    // Weather rows still unanswered -> Next button still disabled
    const nextBtn = wrapper.find('.nav-btn.primary')
    expect(nextBtn.attributes('disabled')).toBeDefined()

    // Answer all 3 weather rows on Item 1
    const weatherRows = tables[1].findAll('tbody tr')
    expect(weatherRows.length).toBe(3)
    for (const row of weatherRows) {
      const cells = row.findAll('.matrix-td')
      await cells[4].trigger('click') // Select rating 5 (Very Suitable)
    }

    // Now all rows for Item 1 are complete -> Next button is unlocked
    expect(nextBtn.attributes('disabled')).toBeUndefined()
    expect(wrapper.find('.incomplete-warning').exists()).toBe(false)

    // Click Next Item to go to Item 2 (Aglio e Olio)
    await nextBtn.trigger('click')
    await flushPromises()

    expect(wrapper.find('.item-tag-pill').text()).toContain('MENU ITEM 2')
    expect(wrapper.text()).toContain('Aglio e Olio')
    expect(wrapper.find('.desc-text').exists()).toBe(false)
    expect(wrapper.find('.desc-tag').exists()).toBe(false)
    expect(wrapper.text()).toContain(
      'How suitable is Aglio e Olio for each of the following moods?',
    )
    expect(wrapper.text()).toContain(
      'How suitable is Aglio e Olio for each of the following weather conditions?',
    )

    // Jump to last item index to test submission flow
    const totalItems = (wrapper.vm as any).menuItems.length
    expect(totalItems).toBeGreaterThan(0)
    ;(wrapper.vm as any).currentItemIndex = totalItems - 1
    await flushPromises()

    expect(wrapper.find('.item-tag-pill').text()).toContain(`MENU ITEM ${totalItems}`)

    // Complete the last item
    const lastTables = wrapper.findAll('.matrix-table')
    const lastMoodRows = lastTables[0].findAll('tbody tr')
    for (const row of lastMoodRows) {
      const cells = row.findAll('.matrix-td')
      await cells[2].trigger('click') // Rating 3
    }
    const lastWeatherRows = lastTables[1].findAll('tbody tr')
    for (const row of lastWeatherRows) {
      const cells = row.findAll('.matrix-td')
      await cells[1].trigger('click') // Rating 2
    }

    const saveBtn = wrapper.find('.nav-btn.success')
    expect(saveBtn.exists()).toBe(true)
    expect(saveBtn.attributes('disabled')).toBeUndefined()
    await saveBtn.trigger('click')
    await flushPromises()

    // Teleported modal check in document.body
    const confirmModalHeading = document.body.querySelector('.modal-card h2')
    expect(confirmModalHeading?.textContent).toBe('Ready to Finish?')

    // Open Review Answers Modal via state or clicking review button
    ;(wrapper.vm as any).showConfirmModal = false
    ;(wrapper.vm as any).showReviewModal = true
    await flushPromises()

    const reviewCard = document.body.querySelector('.review-card')
    expect(reviewCard).not.toBeNull()
    const reviewText = reviewCard?.textContent || ''
    expect(reviewText).toContain('SECTION 1 — Privacy Notice & Consent')
    expect(reviewText).toContain('SECTION 2 — Respondent Information')
    expect(reviewText).toContain('SECTION 3 — Menu Item Evaluations')
    expect(reviewText).toContain('Chicken Alfredo')
    expect(reviewText).toContain('Question 1 — Mood Association')
    expect(reviewText).toContain('Question 2 — Weather Association')
    expect(reviewText).toContain('4 / 5 (Suitable)')
    expect(reviewText).toContain('5 / 5 (Very Suitable)')

    // Submit survey
    await (wrapper.vm as any).executeFinalSubmit()
    await flushPromises()

    expect(axios.post).toHaveBeenCalledWith(
      '/submit-category',
      expect.objectContaining({
        answers: expect.arrayContaining([
          expect.objectContaining({
            menuItemId: 101,
            questionId: expect.any(Number),
            textResponse: expect.stringContaining(
              'Relaxation (Wants to unwind, destress, or enjoy a calm and peaceful moment): 4 (Suitable)',
            ),
          }),
          expect.objectContaining({
            menuItemId: 101,
            questionId: expect.any(Number),
            textResponse: expect.stringContaining(
              'Rainy (Wet, gloomy, or rainy days; craving something warming, cozy, or comforting): 5 (Very Suitable)',
            ),
          }),
        ]),
      }),
    )

    // Success modal appears
    const successHeading = document.body.querySelector('.modal-card h2')
    expect(successHeading?.textContent).toBe('Amazing Job!')
  })

  it('dynamically renders custom evaluation dimensions fetched from backend options', async () => {
    const customQuestions = [
      {
        id: 1,
        text: 'Question 1 — Mood Association: How suitable is this item for each mood?',
        questionType: 'MATRIX',
        options: [
          { id: 101, label: 'Late Night Snack', sub: '(For midnight cravings)' },
          { id: 102, label: 'Post-Workout', sub: '(To refuel after exercise)' },
        ],
      },
      {
        id: 2,
        text: 'Question 2 — Weather Association: How suitable is this item in this weather?',
        questionType: 'MATRIX',
        options: [
          { id: 201, label: 'Tropical Storm', icon: '⛈️' },
        ],
      },
    ]

    ;(axios.get as any).mockImplementation((url: string) => {
      if (url.startsWith('/menu-items')) return Promise.resolve({ data: [mockMenuItems[0]] })
      if (url === '/questions/all') return Promise.resolve({ data: customQuestions })
      if (url === '/api/stats/survey-status') return Promise.resolve({ data: { isFull: false } })
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(Survey)
    await flushPromises()

    // Start survey & accept privacy
    await wrapper.find('.primary-btn.pulse').trigger('click')
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')

    // Fill demographics and go to Section 3
    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')
    ;(wrapper.vm as any).showInstructionsModal = false
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Check that custom dimension labels appear in the rendered matrix table
    expect(wrapper.find('.rating-view').exists()).toBe(true)
    expect(wrapper.text()).toContain('Late Night Snack')
    expect(wrapper.text()).toContain('Post-Workout')
    expect(wrapper.text()).toContain('Tropical Storm')
    expect(wrapper.text()).toContain('⛈️')
  })

  it('allows unchecking/deselecting radio buttons by clicking them again', async () => {
    const wrapper = mount(Survey)
    await flushPromises()

    // Start Survey -> Section 1
    await wrapper.find('.primary-btn.pulse').trigger('click')
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')

    // In Section 2 (Demographics), click an age group option
    const demoButtons = wrapper.findAll('.demo-opt-btn')
    expect((wrapper.vm as any).demographicAnswers.ageGroup).toBe('')
    await demoButtons[0].trigger('click')
    expect((wrapper.vm as any).demographicAnswers.ageGroup).not.toBe('')
    // Click same age group option again -> should uncheck / clear
    await demoButtons[0].trigger('click')
    expect((wrapper.vm as any).demographicAnswers.ageGroup).toBe('')

    // Select demographics to proceed to Section 3
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')
    ;(wrapper.vm as any).showInstructionsModal = false
    await wrapper.vm.$nextTick()
    await flushPromises()

    const tables = wrapper.findAll('.matrix-table')
    const firstMoodRow = tables[0].find('tbody tr')
    const cells = firstMoodRow.findAll('.matrix-td')

    // Click rating 1 (scale index 0)
    await cells[0].trigger('click')
    const radioCircle = cells[0].find('.grid-radio-circle')
    expect(radioCircle.classes()).toContain('active')
    expect(firstMoodRow.classes()).toContain('row-answered')

    // Click rating 1 again -> should uncheck it!
    await cells[0].trigger('click')
    expect(radioCircle.classes()).not.toContain('active')
    expect(firstMoodRow.classes()).not.toContain('row-answered')

    // Click rating 2 (scale index 1)
    await cells[1].trigger('click')
    expect(cells[1].find('.grid-radio-circle').classes()).toContain('active')
    expect(firstMoodRow.classes()).toContain('row-answered')

    // Now test Weather table
    const firstWeatherRow = tables[1].find('tbody tr')
    const weatherCells = firstWeatherRow.findAll('.matrix-td')
    await weatherCells[2].trigger('click')
    expect(weatherCells[2].find('.grid-radio-circle').classes()).toContain('active')

    // Click rating 3 on Weather row again -> should uncheck it!
    await weatherCells[2].trigger('click')
    expect(weatherCells[2].find('.grid-radio-circle').classes()).not.toContain('active')
    expect(firstWeatherRow.classes()).not.toContain('row-answered')
  })

  it('warns user before closing/reloading when survey is in progress', async () => {
    const wrapper = mount(Survey)
    await flushPromises()

    // 1. Initially on welcome screen -> beforeunload does NOT trigger
    const initialEvent = new Event('beforeunload') as BeforeUnloadEvent
    initialEvent.preventDefault = vi.fn()
    window.dispatchEvent(initialEvent)
    expect(initialEvent.preventDefault).not.toHaveBeenCalled()

    // 2. Start survey & consent to privacy -> beforeunload triggers
    await wrapper.find('.primary-btn.pulse').trigger('click')
    await wrapper.find('.consent-card').trigger('click')

    const inProgressEvent = new Event('beforeunload') as BeforeUnloadEvent
    inProgressEvent.preventDefault = vi.fn()
    window.dispatchEvent(inProgressEvent)
    expect(inProgressEvent.preventDefault).toHaveBeenCalled()

    // 3. Complete survey -> beforeunload does NOT trigger
    ;(wrapper.vm as any).showSuccessModal = true
    await wrapper.vm.$nextTick()

    const submittedEvent = new Event('beforeunload') as BeforeUnloadEvent
    submittedEvent.preventDefault = vi.fn()
    window.dispatchEvent(submittedEvent)
    expect(submittedEvent.preventDefault).not.toHaveBeenCalled()

    // 4. Clean up on unmount
    wrapper.unmount()
    const afterUnmountEvent = new Event('beforeunload') as BeforeUnloadEvent
    afterUnmountEvent.preventDefault = vi.fn()
    window.dispatchEvent(afterUnmountEvent)
    expect(afterUnmountEvent.preventDefault).not.toHaveBeenCalled()
  })

  it('opens and closes high-resolution photo zoom modal upon clicking image or escape key', async () => {
    const wrapper = mount(Survey)
    await flushPromises()

    // Start survey -> Section 1 -> Section 2 -> Section 3
    await wrapper.find('.primary-btn.pulse').trigger('click')
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')
    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')
    ;(wrapper.vm as any).showInstructionsModal = false
    await wrapper.vm.$nextTick()
    await flushPromises()

    // Initially zoom modal is not open
    expect(document.body.querySelector('.zoom-modal-card')).toBeNull()

    // Click cover image to open zoom modal
    const coverImg = wrapper.find('.cover-img')
    expect(coverImg.exists()).toBe(true)
    await coverImg.trigger('click')
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Zoom modal should now be present in DOM
    const zoomCard = document.body.querySelector('.zoom-modal-card')
    expect(zoomCard).not.toBeNull()
    expect(zoomCard?.textContent).toContain('Chicken Alfredo')

    // Click close button
    const closeBtn = document.body.querySelector('.zoom-close-btn') as HTMLButtonElement
    expect(closeBtn).not.toBeNull()
    closeBtn.click()
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(document.body.querySelector('.zoom-modal-card')).toBeNull()

    // Reopen and test Escape key
    await coverImg.trigger('click')
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(document.body.querySelector('.zoom-modal-card')).not.toBeNull()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(document.body.querySelector('.zoom-modal-card')).toBeNull()
  })

  it('requires all mood and weather rows to be answered before unlocking Next Item button', async () => {
    const wrapper = mount(Survey)
    await flushPromises()

    // Start Survey -> Section 1 -> Section 2 -> Section 3
    await wrapper.find('.primary-btn.pulse').trigger('click')
    await wrapper.find('.consent-card').trigger('click')
    await wrapper.find('.privacy-proceed-btn').trigger('click')
    const demoButtons = wrapper.findAll('.demo-opt-btn')
    await demoButtons[0].trigger('click')
    await demoButtons[5].trigger('click')
    await wrapper.find('.demo-proceed-btn').trigger('click')
    ;(wrapper.vm as any).showInstructionsModal = false
    await wrapper.vm.$nextTick()
    await flushPromises()

    const tables = wrapper.findAll('.matrix-table')
    const nextBtn = wrapper.find('.nav-btn.primary')

    // Initially disabled
    expect(nextBtn.attributes('disabled')).toBeDefined()

    // Answer ONLY 1 mood row out of 7
    const moodRows = tables[0].findAll('tbody tr')
    await moodRows[0].findAll('.matrix-td')[2].trigger('click')
    expect(nextBtn.attributes('disabled')).toBeDefined()

    // Answer remaining mood rows (all 7 mood rows now answered)
    for (let i = 1; i < moodRows.length; i++) {
      await moodRows[i].findAll('.matrix-td')[2].trigger('click')
    }
    // Still disabled because 0 weather rows answered
    expect(nextBtn.attributes('disabled')).toBeDefined()

    // Answer ONLY 1 weather row out of 3
    const weatherRows = tables[1].findAll('tbody tr')
    await weatherRows[0].findAll('.matrix-td')[3].trigger('click')
    // Still disabled because not all weather rows are answered
    expect(nextBtn.attributes('disabled')).toBeDefined()

    // Answer remaining weather rows
    for (let i = 1; i < weatherRows.length; i++) {
      await weatherRows[i].findAll('.matrix-td')[3].trigger('click')
    }

    // Now unlocked because all mood and weather rows are answered!
    expect(nextBtn.attributes('disabled')).toBeUndefined()
  })

  it('displays survey limit modal if no available menu items are returned', async () => {
    ;(axios.get as any).mockImplementation((url: string) => {
      if (url.startsWith('/menu-items')) {
        return Promise.resolve({ data: [] })
      }
      return Promise.resolve({ data: [] })
    })

    const wrapper = mount(Survey)
    await flushPromises()

    expect((wrapper.vm as any).showLimitModal).toBe(true)
  })
})


