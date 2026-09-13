import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import axios from 'axios'
import Survey from '../Survey.vue'

vi.mock('axios')

const mockMenuItems = [
  { id: 101, name: 'Chicken Alfredo', category: 'PASTA', price: 220, imageName: 'chicken_alfredo.jpg' },
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
      if (url === '/menu-items') {
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
    expect(starterText).toContain('Instructions')
    expect(starterText).toContain('You will be asked to evaluate 10 menu items.')
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
      'Creamy pasta with chicken and Alfredo sauce.',
    )

    // Verify Question 1 — Mood Association
    expect(wrapper.text()).toContain('Question 1 — Mood Association')
    expect(wrapper.text()).toContain(
      'How suitable is Chicken Alfredo for each of the following moods?',
    )

    const moodLabels = [
      'Energy (Wants something energizing)',
      'Comfort (Wants something warm or familiar)',
      'Refreshing (Wants something light or cooling)',
      'Healthy (Wants a healthier choice)',
      'Treat (Wants something enjoyable or indulgent)',
      'Focused (Wants to concentrate or study)',
      'Familiar (Wants a safe, familiar choice)',
      'Adventurous (Wants to try something new)',
      'Quick (Wants something convenient)',
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
      'Hot/Sunny',
      'Hot/Humid',
      'Rainy',
      'Cool Dry (Note: Even in tropical climates, "cool dry" exists: breezy December–February days, air-conditioned spaces, or cool hill stations/evening breezes.)',
    ]

    weatherLabels.forEach((weather) => {
      expect(wrapper.text()).toContain(weather)
    })

    // Check "Require a response in each row" notices
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

    // Answer all 9 mood rows on Item 1 (Chicken Alfredo)
    const tables = wrapper.findAll('.matrix-table')
    expect(tables.length).toBe(2)

    const moodRows = tables[0].findAll('tbody tr')
    expect(moodRows.length).toBe(9)
    for (const row of moodRows) {
      const cells = row.findAll('.matrix-td')
      await cells[3].trigger('click') // Select rating 4 (Suitable)
    }

    // Weather rows still unanswered -> Next button still disabled
    const nextBtn = wrapper.find('.nav-btn.primary')
    expect(nextBtn.attributes('disabled')).toBeDefined()

    // Answer all 4 weather rows on Item 1
    const weatherRows = tables[1].findAll('tbody tr')
    expect(weatherRows.length).toBe(4)
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
    expect(wrapper.find('.desc-text').text()).toBe(
      'Classic pasta tossed in sautéed garlic, extra virgin olive oil, and chili flakes.',
    )
    expect(wrapper.text()).toContain(
      'How suitable is Aglio e Olio for each of the following moods?',
    )
    expect(wrapper.text()).toContain(
      'How suitable is Aglio e Olio for each of the following weather conditions?',
    )

    // Jump to last item index to test submission flow
    const totalItems = (wrapper.vm as any).menuItems.length
    expect(totalItems).toBe(10)
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
              'Energy (Wants something energizing): 4 (Suitable)',
            ),
          }),
          expect.objectContaining({
            menuItemId: 101,
            questionId: expect.any(Number),
            textResponse: expect.stringContaining('Hot/Sunny: 5 (Very Suitable)'),
          }),
        ]),
      }),
    )

    // Success modal appears
    const successHeading = document.body.querySelector('.modal-card h2')
    expect(successHeading?.textContent).toBe('Amazing Job!')
  })
})
