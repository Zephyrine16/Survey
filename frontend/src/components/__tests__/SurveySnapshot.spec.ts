import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import axios from 'axios'
import questionsSnapshot from '../../generated/questions-snapshot.json'

vi.mock('axios')
vi.mock('../../generated/menu-snapshot.json', () => ({
  default: [{ id: 101, name: 'Chicken Alfredo', category: 'PASTA', imageName: null }],
}))

import Survey from '../Survey.vue'

enableAutoUnmount(afterEach)

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  window.scrollTo = vi.fn()
})

it('shows the bundled menu to a first-time visitor while the backend is asleep', async () => {
  ;(axios.get as any).mockImplementation((url: string) => {
    if (url.startsWith('/menu-items')) return new Promise(() => {})
    return Promise.resolve({ data: [] })
  })

  const wrapper = mount(Survey)
  await wrapper.find('.primary-btn.pulse').trigger('click')
  await wrapper.find('.consent-card').trigger('click')
  await wrapper.find('.privacy-proceed-btn').trigger('click')
  const demoButtons = wrapper.findAll('.demo-opt-btn')
  await demoButtons[0].trigger('click')
  await demoButtons[5].trigger('click')
  expect(wrapper.find('.demo-proceed-btn').attributes('disabled')).toBeUndefined()

  await wrapper.find('.demo-proceed-btn').trigger('click')
  ;(wrapper.vm as any).showInstructionsModal = false
  await wrapper.vm.$nextTick()
  expect(wrapper.text()).toContain('Chicken Alfredo')
  expect(wrapper.find('.menu-sync-notice').text()).toContain('saved menu')
  expect((wrapper.vm as any).moodRows.map((row: any) => row.short)).toEqual(
    questionsSnapshot[0].options.map((option) => option.label),
  )
  expect((wrapper.vm as any).weatherRows.map((row: any) => row.short)).toEqual(
    questionsSnapshot[1].options.map((option) => option.label),
  )
})

it('keeps ratings on their original item and updates the menu without a reload', async () => {
  let resolveAvailable!: (value: { data: any[] }) => void
  const liveQuestions = structuredClone(questionsSnapshot) as any[]
  liveQuestions[0].options[0].id = 118
  liveQuestions[0].options.push({ id: 119, label: 'New Mood', sub: null, icon: null })
  liveQuestions[1].options[1].id = 107
  ;(axios.get as any).mockImplementation((url: string) => {
    if (url === '/menu-items?availableOnly=true') {
      return new Promise((resolve) => { resolveAvailable = resolve })
    }
    if (url === '/menu-items') {
      return Promise.resolve({ data: [
        { id: 101, name: 'Chicken Alfredo — updated', category: 'PASTA', imageName: null },
        { id: 102, name: 'New Item', category: 'PASTA', imageName: null },
      ] })
    }
    if (url === '/questions/all') return Promise.resolve({ data: liveQuestions })
    return Promise.resolve({ data: { isFull: false } })
  })
  ;(axios.post as any).mockResolvedValue({ data: { message: 'Saved' } })

  const wrapper = mount(Survey)
  await wrapper.find('.primary-btn.pulse').trigger('click')
  ;(wrapper.vm as any).currentSection = 3
  ;(wrapper.vm as any).setMoodAnswer(101, 'happy', 4)
  ;(wrapper.vm as any).setWeatherAnswer(101, 'rainy', 5)
  await wrapper.vm.$nextTick()
  expect(wrapper.text()).toContain('Chicken Alfredo')
  const originalMoodRows = (wrapper.vm as any).moodRows.map((row: any) => row.label)
  const originalWeatherRows = (wrapper.vm as any).weatherRows.map((row: any) => row.label)

  resolveAvailable({ data: [{ id: 102, name: 'New Item', category: 'PASTA', imageName: null }] })
  await new Promise((resolve) => setTimeout(resolve, 20))
  await wrapper.vm.$nextTick()

  expect((wrapper.vm as any).menuLoadState).toBe('ready')
  expect(wrapper.text()).toContain('Chicken Alfredo — updated')
  expect((wrapper.vm as any).menuItems[0].id).toBe(101)
  expect((wrapper.vm as any).getMoodAnswer(101, 'happy')).toBe(4)
  expect((wrapper.vm as any).getWeatherAnswer(101, 'rainy')).toBe(5)
  expect((wrapper.vm as any).moodRows.map((row: any) => row.label)).toEqual(originalMoodRows)
  expect((wrapper.vm as any).weatherRows.map((row: any) => row.label)).toEqual(originalWeatherRows)
  expect((wrapper.vm as any).moodRows[0].dbOptionId).toBe(118)
  expect((wrapper.vm as any).weatherRows[1].dbOptionId).toBe(107)

  await (wrapper.vm as any).executeFinalSubmit()
  expect(axios.post).toHaveBeenCalledWith('/submit-category', expect.objectContaining({
    answers: expect.arrayContaining([
      expect.objectContaining({ menuItemId: 101, selectedOptionId: 118, textResponse: expect.stringContaining('Happy: 4') }),
      expect.objectContaining({ menuItemId: 101, selectedOptionId: 107, textResponse: expect.stringContaining('Rainy: 5') }),
    ]),
  }))
})
