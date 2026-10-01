import { afterEach, expect, it, vi } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import axios from 'axios'

vi.mock('axios')
vi.mock('../../generated/menu-snapshot.json', () => ({
  default: [{ id: 101, name: 'Chicken Alfredo', category: 'PASTA', imageName: null }],
}))

import Survey from '../Survey.vue'

enableAutoUnmount(afterEach)

it('shows the bundled menu to a first-time visitor while the backend is asleep', async () => {
  localStorage.clear()
  window.scrollTo = vi.fn()
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
})
